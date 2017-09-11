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

import java.io.IOException;
import java.util.Arrays;

import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.salesforce.apt.graph.model.DefinitionModel;
import com.salesforce.apt.graph.model.ExpectedModel;
import com.salesforce.apt.graph.model.InstanceDependencyModel;
import com.salesforce.apt.graph.model.InstanceModel;

public class TestReadWrite {

  private static final String TEST_DEF1 = "test.Def1";
  private static final String TEST_DEF2 = "test.Def2";
  //private static final String TEST_DEF3 = "test.Def3";
  //private static final String TEST_DEF4 = "test.Def4";
  
  private static final String OBJECT1 = "object1";
  private static final String OBJECT1_SOURCE = "test.Def1.object1(...)";
  
  private static final String OBJECT2 = "object2";
  private static final String OBJECT2_SOURCE = "test.Def2.object2(...)";
  
  //private static final String OBJECT3 = "object3";
  //private static final String OBJECT3_SOURCE = "test.Def3.object3(...)";
  
  //private static final String TYPE_CHARSEQUENCE = "java.lang.CharSequence";
  private static final String TYPE_STRING = "java.lang.String";
  //private static final String TYPE_STRINGBUILDER = "java.lang.StringBuilder";

  
  @Test
  public void testReadWrite() throws IOException {
    DefinitionModel model1 = new DefinitionModel(TEST_DEF1);
    model1.addDefinition(new ExpectedModel(OBJECT2));
    model1.addDefinition(new InstanceModel(OBJECT1, TEST_DEF1, OBJECT1_SOURCE, TYPE_STRING,
        Arrays.asList(new InstanceDependencyModel(OBJECT2, TYPE_STRING)), Arrays.asList()));
    DefinitionModel model2 = new DefinitionModel(TEST_DEF2);
    model2.addDependencyNames(TEST_DEF1);
    model2.addDefinition(new InstanceModel(OBJECT2, TEST_DEF2, OBJECT2_SOURCE, TYPE_STRING, Arrays.asList(), Arrays.asList()));
    Gson gson = new GsonBuilder().create();
    String output = gson.toJson(model1);
    System.out.println(output);
    DefinitionModel model1FromJson = gson.fromJson(output, DefinitionModel.class);
    //TODO: assert more, or implement a real equals/hash methods for these classes.
    assertThat(model1.getExpectedDefinitions().get(0).getIdentity())
      .isEqualTo(model1FromJson.getExpectedDefinitions().get(0).getIdentity());    
  }
  
}
