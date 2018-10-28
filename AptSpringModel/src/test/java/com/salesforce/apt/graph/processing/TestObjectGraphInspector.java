/*
 * Copyright © 2017, Salesforce.com, Inc
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
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.salesforce.apt.graph.model.DefinitionModel;
import com.salesforce.apt.graph.model.ExpectedModel;
import com.salesforce.apt.graph.model.InstanceDependencyModel;
import com.salesforce.apt.graph.model.InstanceModel;
import com.salesforce.apt.graph.model.errors.ErrorType;
import com.salesforce.apt.graph.model.storage.DefinitionModelStore;
import com.salesforce.apt.graph.model.storage.ResourceLoader;
import com.salesforce.apt.graph.model.storage.classpath.ClasspathUrlResourceLoader;
import com.salesforce.apt.graph.model.storage.classpath.GsonDefinitionModelStore;
import com.salesforce.apt.graph.model.storage.classpath.TestFileStore;
import com.salesforce.apt.graph.test.cycle.definitions.TestErrorListener;
import com.salesforce.apt.graph.types.impl.ReflectionAssignabilityUtils;

public class TestObjectGraphInspector {
  
  
  private static final String TEST_DEF1 = "test.Def1";
  private static final String TEST_DEF2 = "test.Def2";
  //private static final String TEST_DEF3 = "test.Def3";
  //private static final String TEST_DEF4 = "test.Def4";
  
  private static final String OBJECT1 = "object1";
  private static final String OBJECT1_SOURCE = "test.Def1.object1(...)";
  
  private static final String OBJECT2 = "object2";
  private static final String OBJECT2_SOURCE = "test.Def2.object2(...)";
  
  private static final String OBJECT3 = "object3";
  //private static final String OBJECT3_SOURCE = "test.Def3.object3(...)";
  
  //private static final String TYPE_CHARSEQUENCE = "java.lang.CharSequence";
  private static final String TYPE_STRING = "java.lang.String";
  //private static final String TYPE_STRINGBUILDER = "java.lang.StringBuilder";

  @Rule
  public TemporaryFolder testFolder = new TemporaryFolder();

  private static DefinitionModelStore getDefinitionModelStore(File root) {
    TestFileStore fileStore = new TestFileStore(root);
    ResourceLoader loader = new ClasspathUrlResourceLoader(root);
    return new GsonDefinitionModelStore(loader, fileStore);
  }
  
  @Test
  public void inspectGraphProperlyListsExpectedEntities() {
    DefinitionModel model1 = new DefinitionModel(TEST_DEF1);
    model1.addDefinition(new ExpectedModel(OBJECT2));
    model1.addDefinition(new InstanceModel(OBJECT1, TEST_DEF1, OBJECT1_SOURCE, TYPE_STRING,
        Arrays.asList(new InstanceDependencyModel(OBJECT2, TYPE_STRING)), Arrays.asList()));
    DefinitionModel model2 = new DefinitionModel(TEST_DEF2);
    model2.addDependencyNames(TEST_DEF1);
    model2.addDefinition(new InstanceModel(OBJECT2, TEST_DEF2, OBJECT2_SOURCE, TYPE_STRING, Arrays.asList(), Arrays.asList()));
    TestErrorListener el = new TestErrorListener();
    List<DefinitionModel> definitions = Arrays.asList(model1, model2);
    DefinitionModelStore store = getDefinitionModelStore(testFolder.getRoot());
    new DefinitionJoiner().joinDefinitions(definitions, store, el);
    assertThat(el.getErrors()).isEmpty();
    Set<DefinitionModel> heads = new DefinitionGraphInpector().inspectDefinitionGraph(definitions, el);
    assertThat(el.getErrors()).isEmpty();
    new DefinitionContentInspector().inspectDefinitionGraph(heads, el, new ReflectionAssignabilityUtils(), store);
    assertThat(el.getErrors()).hasSize(0);
  }
  
  @Test
  public void inspectGraphWithDuplicateIdentities() {
    DefinitionModel model1 = new DefinitionModel(TEST_DEF1);
    InstanceModel instance1 = new InstanceModel(OBJECT1, TEST_DEF1, OBJECT1_SOURCE, TYPE_STRING, Arrays.asList(), Arrays.asList());
    model1.addDefinition(instance1);
    DefinitionModel model2 = new DefinitionModel(TEST_DEF2);
    model2.addDependencyNames(TEST_DEF1);
    InstanceModel instance2 = new InstanceModel(OBJECT1, TEST_DEF2, OBJECT2_SOURCE, TYPE_STRING, Arrays.asList(), Arrays.asList());
    model2.addDefinition(instance2);
    TestErrorListener el = new TestErrorListener();
    List<DefinitionModel> definitions = Arrays.asList(model1, model2);
    DefinitionModelStore store = getDefinitionModelStore(testFolder.getRoot());
    new DefinitionJoiner().joinDefinitions(definitions, store, el);
    assertThat(el.getErrors()).isEmpty();
    Set<DefinitionModel> heads = new DefinitionGraphInpector().inspectDefinitionGraph(definitions, el);
    assertThat(el.getErrors()).isEmpty();
    new DefinitionContentInspector().inspectDefinitionGraph(heads, el, new ReflectionAssignabilityUtils(), store);
    assertThat(el.getErrors()).hasSize(1);
    /*
     * Since model2 depends on model1 and model2 has the duplicate bean name,
     * the error will always be reported on model2, and model1 if it has a SourceElement.
     */
    assertThat(el.getErrors().get(0).getMessage()).isEqualTo(ErrorType.DUPLICATE_OBJECT_DEFINITIONS);
    assertThat(el.getErrors().get(0).getInvolved()).containsOnly(model2); 
    assertThat(el.getErrors().get(0).getCauses()).containsOnly(instance1, instance2);
  }
  
  @Test
  public void inspectWithMissingExpectedIdentities() {
    DefinitionModelStore store = getDefinitionModelStore(testFolder.getRoot());

    DefinitionModel model1 = new DefinitionModel(TEST_DEF1);
    InstanceModel instance1 = new InstanceModel(OBJECT1, TEST_DEF1, OBJECT1_SOURCE, TYPE_STRING,
        Arrays.asList(new InstanceDependencyModel(OBJECT2, TYPE_STRING)), Arrays.asList());
    model1.addDefinition(instance1);
    TestErrorListener el = new TestErrorListener();
    List<DefinitionModel> definitions = Arrays.asList(model1);
    new DefinitionJoiner().joinDefinitions(definitions, store, el);
    assertThat(el.getErrors()).isEmpty();
    Set<DefinitionModel> heads = new DefinitionGraphInpector().inspectDefinitionGraph(definitions, el);
    assertThat(el.getErrors()).isEmpty();
    new DefinitionContentInspector().inspectDefinitionGraph(heads, el, new ReflectionAssignabilityUtils(), store);
    /*
     * Since model1 has an instance, object1 that depends on and expected object2, that is not
     * declared by model1 as an expected instance.
     */
    assertThat(el.getErrors()).hasSize(1);
    assertThat(el.getErrors().get(0).getMessage()).isEqualTo(ErrorType.MISSING_BEAN_DEFINITIONS);
    assertThat(el.getErrors().get(0).getInvolved()).containsOnly(model1); 
    assertThat(el.getErrors().get(0).getCauses()).hasSize(1);
    assertThat(el.getErrors().get(0).getCauses().get(0).getIdentity()).isEqualTo(OBJECT2);
  }
  
  @Test
  public void inspectWithUnneccessartExpectedIdentities() {
    DefinitionModelStore store = getDefinitionModelStore(testFolder.getRoot());
    
    DefinitionModel model1 = new DefinitionModel(TEST_DEF1);
    ExpectedModel expected1 = new ExpectedModel("OBJECT2");
    model1.addDefinition(expected1);
    InstanceModel instance1 = new InstanceModel(OBJECT1, TEST_DEF1, OBJECT1_SOURCE, TYPE_STRING,
        Arrays.asList(), Arrays.asList());
    model1.addDefinition(instance1);
    TestErrorListener el = new TestErrorListener();
    List<DefinitionModel> definitions = Arrays.asList(model1);
    new DefinitionJoiner().joinDefinitions(definitions, store, el);
    assertThat(el.getErrors()).isEmpty();
    Set<DefinitionModel> heads = new DefinitionGraphInpector().inspectDefinitionGraph(definitions, el);
    assertThat(el.getErrors()).isEmpty();
    new DefinitionContentInspector().inspectDefinitionGraph(heads, el, new ReflectionAssignabilityUtils(), store);
    /*
     * Since model1 has an instance, object1 that depends on and expected object2, that is not
     * declared by model1 as an expected instance.
     */
    assertThat(el.getErrors()).hasSize(1);
    assertThat(el.getErrors().get(0).getMessage()).isEqualTo(ErrorType.UNUSED_EXPECTED);
    assertThat(el.getErrors().get(0).getInvolved()).containsOnly(model1); 
    assertThat(el.getErrors().get(0).getCauses()).containsOnly(expected1);
  }
  
  @Test
  public void inspectWithAliasesFulfillingUsage() {
    DefinitionModel model1 = new DefinitionModel(TEST_DEF1);
    InstanceModel instance1 = new InstanceModel(OBJECT1, TEST_DEF1, OBJECT1_SOURCE, TYPE_STRING,
        Arrays.asList(new InstanceDependencyModel(OBJECT3, TYPE_STRING)), Arrays.asList());
    InstanceModel instance2 = new InstanceModel(OBJECT2, TEST_DEF1, OBJECT2_SOURCE, TYPE_STRING,
        Arrays.asList(), Arrays.asList(OBJECT3));
    model1.addDefinition(instance1);
    model1.addDefinition(instance2);
    TestErrorListener el = new TestErrorListener();
    List<DefinitionModel> definitions = Arrays.asList(model1);
    DefinitionModelStore store = getDefinitionModelStore(testFolder.getRoot());
    new DefinitionJoiner().joinDefinitions(definitions, store, el);
    assertThat(el.getErrors()).isEmpty();
    Set<DefinitionModel> heads = new DefinitionGraphInpector().inspectDefinitionGraph(definitions, el);
    assertThat(el.getErrors()).isEmpty();
    new DefinitionContentInspector().inspectDefinitionGraph(heads, el, new ReflectionAssignabilityUtils(), store);
    assertThat(el.getErrors()).hasSize(0);
  }

  @Test
  public void inspectWithAliasesCauseDuplicateObjects() {
    DefinitionModel model1 = new DefinitionModel(TEST_DEF1);
    InstanceModel instance1 = new InstanceModel(OBJECT1, TEST_DEF1, OBJECT1_SOURCE, TYPE_STRING,
        Arrays.asList(new InstanceDependencyModel(OBJECT3, TYPE_STRING)), Arrays.asList());
    InstanceModel instance2 = new InstanceModel(OBJECT2, TEST_DEF1, OBJECT2_SOURCE, TYPE_STRING,
        Arrays.asList(), Arrays.asList(OBJECT1));
    model1.addDefinition(instance1);
    model1.addDefinition(instance2);
    TestErrorListener el = new TestErrorListener();
    List<DefinitionModel> definitions = Arrays.asList(model1);
    DefinitionModelStore store = getDefinitionModelStore(testFolder.getRoot());
    new DefinitionJoiner().joinDefinitions(definitions, store, el);
    assertThat(el.getErrors()).isEmpty();
    Set<DefinitionModel> heads = new DefinitionGraphInpector().inspectDefinitionGraph(definitions, el);
    assertThat(el.getErrors()).isEmpty();
    new DefinitionContentInspector().inspectDefinitionGraph(heads, el, new ReflectionAssignabilityUtils(), store);
    /*
     * Since model1 has an instance, object1 that depends on and expected object2, that is not
     * declared by model1 as an expected instance.
     */
    assertThat(el.getErrors()).hasSize(1);
    assertThat(el.getErrors().get(0).getMessage()).isEqualTo(ErrorType.DUPLICATE_OBJECT_DEFINITIONS);
    assertThat(el.getErrors().get(0).getInvolved()).containsOnly(model1); 
    assertThat(el.getErrors().get(0).getCauses()).containsOnly(instance1, instance2);
  }
}
