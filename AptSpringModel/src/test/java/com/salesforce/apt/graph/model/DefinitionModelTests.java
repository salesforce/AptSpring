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
package com.salesforce.apt.graph.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.salesforce.apt.graph.model.storage.DefinitionModelStore;
import com.salesforce.apt.graph.model.storage.ResourceLoader;
import com.salesforce.apt.graph.model.storage.classpath.ClasspathUrlResourceLoader;
import com.salesforce.apt.graph.model.storage.classpath.GsonDefinitionModelStore;
import com.salesforce.apt.graph.model.storage.classpath.TestFileStore;
import com.salesforce.apt.graph.processing.DefinitionJoiner;
import com.salesforce.apt.graph.test.cycle.definitions.TestErrorListener;

public class DefinitionModelTests {

  private static final String TEST_DEF1 = "test.Def1";
  private static final String TEST_DEF2 = "test.Def2";
  
  private static final String OBJECT1 = "object1";
  
  private static final String OBJECT2 = "object2";
  
  @Rule
  public TemporaryFolder testFolder = new TemporaryFolder();

  private static DefinitionModelStore getDefinitionModelStore(File root) {
    TestFileStore fileStore = new TestFileStore(root);
    ResourceLoader loader = new ClasspathUrlResourceLoader(root);
    return new GsonDefinitionModelStore(loader, fileStore);
  }

  @Test
  public void testLockReadOnExpectedFalse() {
    DefinitionModel model1 = new DefinitionModel(TEST_DEF1);
    model1.addDependencyNames(Arrays.asList(TEST_DEF2));
    ExpectedModel expected1 = new ExpectedModel(OBJECT1);
    model1.addDefinition(expected1);
    assertThat(model1.isComplete()).isFalse();
    ExpectedModel expected2 = new ExpectedModel(OBJECT2);
    assertThatThrownBy(() -> model1.addDefinition(expected2))
      .hasMessage("Attempting to modify 'source read' content after source read is locked")
      .isInstanceOf(RuntimeException.class);
  }

  
  @Test
  public void testIsCompleteWithNoExpected() {
    DefinitionModel model1 = new DefinitionModel(TEST_DEF1);
    assertThat(model1.isComplete()).isTrue();
  }
  
  
  @Test
  public void addDependencies() {
    DefinitionModel model1 = new DefinitionModel(TEST_DEF1);
    model1.addDependencyNames(Arrays.asList(TEST_DEF2));
    DefinitionModel model2 = new DefinitionModel(TEST_DEF2);
    model2.addDependencyNames(Arrays.asList());
    TestErrorListener el = new TestErrorListener();
    List<DefinitionModel> definitions = Arrays.asList(model1, model2);
    new DefinitionJoiner().joinDefinitions(definitions, getDefinitionModelStore(testFolder.getRoot()), el);
    model1.addDependencies(Arrays.asList(model1));
    model1.getDependencies().contains(model1);
    assertThatThrownBy(() -> model1.addDependencies(Arrays.asList(model2)))
      .hasMessage("Attempting to modify 'definition merge' content after definition merge is locked")
      .isInstanceOf(RuntimeException.class);
  }
}
