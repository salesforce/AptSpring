/*
 * Copyright © 2017, Saleforce.com, Inc
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the <organization> nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.salesforce.apt.graph.processing;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.salesforce.apt.graph.model.DefinitionModel;
import com.salesforce.apt.graph.model.errors.ErrorType;
import com.salesforce.apt.graph.model.storage.DefinitionModelStore;
import com.salesforce.apt.graph.model.storage.ResourceLoader;
import com.salesforce.apt.graph.model.storage.classpath.ClasspathUrlResourceLoader;
import com.salesforce.apt.graph.model.storage.classpath.GsonDefinitionModelStore;
import com.salesforce.apt.graph.model.storage.classpath.TestFileStore;
import com.salesforce.apt.graph.test.cycle.definitions.TestErrorListener;

public class TestDefinitionJoiner {

  private static final String TEST_DEF1 = "test.Def1";
  private static final String TEST_DEF2 = "test.Def2";

  
  @Rule
  public TemporaryFolder testFolder = new TemporaryFolder();

  private static DefinitionModelStore getDefinitionModelStore(File root) {
    TestFileStore fileStore = new TestFileStore(root);
    ResourceLoader loader = new ClasspathUrlResourceLoader(root);
    return new GsonDefinitionModelStore(loader, fileStore);
  }
    
  @Test
  public void joinerTest() {
    DefinitionModel model1 = new DefinitionModel(TEST_DEF1);
    model1.addDependencyNames(Arrays.asList(TEST_DEF2));
    DefinitionModel model2 = new DefinitionModel(TEST_DEF2);
    model2.addDependencyNames(Arrays.asList(TEST_DEF1));
    TestErrorListener el = new TestErrorListener();
    new DefinitionJoiner().joinDefinitions(Arrays.asList(model1, model2), getDefinitionModelStore(testFolder.getRoot()), el);
    assertThat(el.getErrors()).isEmpty();
    assertThat(model1.getDependencies()).containsOnly(model2);
    assertThat(model2.getDependencies()).containsOnly(model1);
  }
  
  @Test
  @Ignore
  public void joinerTestWithStoredDate() throws IOException {
    DefinitionModel model1 = new DefinitionModel(TEST_DEF1);
    model1.addDependencyNames(Arrays.asList(TEST_DEF2));
    DefinitionModel model2 = new DefinitionModel(TEST_DEF2);
    model2.addDependencyNames(Arrays.asList(TEST_DEF1));
    TestErrorListener el = new TestErrorListener();
    
    DefinitionModelStore store = new DefinitionModelStore() {
      Map<String, List<DefinitionModel>> models = new HashMap<>();
      
      @Override
      public boolean store(DefinitionModel model) {
        List<DefinitionModel> list = models.getOrDefault(model.getIdentity(), new ArrayList<>());
        list.add(model);
        models.put(model.getIdentity(), list);
        return true;
      }
      
      @Override
      public List<DefinitionModel> lookup(String name) {
        return models.get(name);
      }
    };

    store.store(new DefinitionModel(TEST_DEF1));
    store.store(new DefinitionModel(TEST_DEF2));
    //ResourceLoader loader = x -> x.equals(TEST_DEF1) ? Arrays.asList(model1) : Arrays.asList(model2), el);
    
    new DefinitionJoiner().joinDefinitions(Arrays.asList(model1, model2), 
        store, el);
    assertThat(el.getErrors().stream().map(s -> s.toString())).containsOnly(
          "DUPLICATED_MATCHING_DEFINITIONS on [test.Def1] caused by [test.Def1, test.Def1]",
          "DUPLICATED_MATCHING_DEPENDENCIES on [test.Def1] caused by [test.Def2, test.Def2]",
          "DUPLICATED_MATCHING_DEFINITIONS on [test.Def2] caused by [test.Def2, test.Def2]",
          "DUPLICATED_MATCHING_DEPENDENCIES on [test.Def2] caused by [test.Def1, test.Def1]"
    );
  }
  
  @Test
  public void joinerMissingDepTest() {
    DefinitionModel model1 = new DefinitionModel(TEST_DEF1);
    model1.addDependencyNames(Arrays.asList(TEST_DEF2));
    TestErrorListener el = new TestErrorListener();
    new DefinitionJoiner().joinDefinitions(Arrays.asList(model1), getDefinitionModelStore(testFolder.getRoot()), el);
    assertThat(el.getErrors()).hasSize(1);
    assertThat(el.getErrors().get(0).getMessage()).isEqualByComparingTo(ErrorType.MISSING_NECESSARY_ANNOTATIONS);
    assertThat(el.getErrors().get(0).getInvolved()).hasSize(1).allMatch(am -> am.getIdentity().equals(TEST_DEF1));
    assertThat(el.getErrors().get(0).getCauses()).hasSize(1).allMatch(am -> am.getIdentity().equals(TEST_DEF2));
  }
  
  @Test
  public void joinerMissingDepTestDifferentJoiner() {
    DefinitionModel model1 = new DefinitionModel(TEST_DEF1);
    model1.addDependencyNames(Arrays.asList(TEST_DEF2));
    TestErrorListener el = new TestErrorListener();
    
    DefinitionModelStore store = new DefinitionModelStore() {
      
      @Override
      public boolean store(DefinitionModel model) {        
        return false;
      }
      
      /**
       * Null has special meaning, it means an io or other read exception occurred.
       */
      @Override
      public List<DefinitionModel> lookup(String name) {
        return null;
      }
    };
    
    new DefinitionJoiner().joinDefinitions(Arrays.asList(model1), store, el);
    assertThat(el.getErrors()).hasSize(1);
    assertThat(el.getErrors()).allMatch(em ->  em.getMessage().equals(ErrorType.COULD_NOT_READ));
    assertThat(el.getErrors()).allMatch(em -> em.getInvolved().stream().allMatch(md -> md.getIdentity().equals(TEST_DEF1)));
  }

  
  @Test
  public void ensureRootNodesAreNotImported() {
    DefinitionModel model1 = new DefinitionModel(TEST_DEF1);
    model1.addDependencyNames(Arrays.asList(TEST_DEF2));
    DefinitionModel model2 = new DefinitionModel(TEST_DEF2, true);
    TestErrorListener el = new TestErrorListener();
    List<DefinitionModel> definitions = Arrays.asList(model1, model2);
    new DefinitionJoiner().joinDefinitions(definitions, getDefinitionModelStore(testFolder.getRoot()), el);
    assertThat(el.getErrors()).hasSize(1);
    assertThat(el.getErrors().get(0).getMessage()).isEqualByComparingTo(ErrorType.ROOT_NODE_IMPORTED);
    assertThat(el.getErrors().get(0).getInvolved()).containsOnly(model1);
    assertThat(el.getErrors().get(0).getCauses()).containsOnly(model2);
  }
  
}
