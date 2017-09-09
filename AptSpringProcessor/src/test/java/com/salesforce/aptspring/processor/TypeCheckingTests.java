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
package com.salesforce.aptspring.processor;

import static com.google.common.truth.Truth.assertAbout;
import static com.google.testing.compile.JavaSourcesSubjectFactory.javaSources;

import java.io.IOException;
import java.util.Arrays;

import javax.tools.JavaFileObject;

import org.junit.Test;

import com.google.testing.compile.JavaFileObjects;

public class TypeCheckingTests {

  private JavaFileObject definitionClassExpectingListString = JavaFileObjects.forSourceLines(
      "test.TestClass1",
      "package test;",
      "",
      "import org.springframework.beans.factory.annotation.Qualifier;",
      "import org.springframework.context.annotation.Bean;",
      "import org.springframework.context.annotation.ComponentScan;",
      "import org.springframework.context.annotation.Configuration;",
      "import org.springframework.context.annotation.Import;",
      "import java.util.List;",
      "",
      "  @com.salesforce.aptspring.Verified()",
      "  @Configuration",
      "  @Import(TestClass2.class)",
      "  public class TestClass1 {",
      "",
      "    @Bean(name = \"value1\")",
      "    public String value1(@Qualifier(\"value2\") String x) { return \"\";}",
      "",
      "    @Bean(name = \"value2\")",
      "    public String value2(@Qualifier(\"value3\") List<String> y) { return \"\";}",
      "",
      "}");
  
  private JavaFileObject definitionClassExpectingListInteger = JavaFileObjects.forSourceLines(
      "test.TestClass1",
      "package test;",
      "",
      "import org.springframework.beans.factory.annotation.Qualifier;",
      "import org.springframework.context.annotation.Bean;",
      "import org.springframework.context.annotation.ComponentScan;",
      "import org.springframework.context.annotation.Configuration;",
      "import org.springframework.context.annotation.Import;",
      "import java.util.List;",
      "",
      "  @com.salesforce.aptspring.Verified()",
      "  @Configuration",
      "  @Import(TestClass2.class)",
      "  public class TestClass1 {",
      "",
      "    @Bean(name = \"value1\")",
      "    public String value1(@Qualifier(\"value2\") String x) { return \"\";}",
      "",
      "    @Bean(name = \"value2\")",
      "    public String value2(@Qualifier(\"value3\") List<Integer> y) { return \"\";}",
      "",
      "}");
  
  JavaFileObject definitionClass2ProvidesListString = JavaFileObjects.forSourceLines(
      "test.TestClass2",
      "package test;",
      "",
      "import org.springframework.beans.factory.annotation.Qualifier;",
      "import org.springframework.context.annotation.Bean;",
      "import org.springframework.context.annotation.ComponentScan;",
      "import org.springframework.context.annotation.Configuration;",
      "import org.springframework.context.annotation.Import;",
      "import java.util.List;",
      "import java.util.ArrayList;",
      "",
      "  @com.salesforce.aptspring.Verified",
      "  @Configuration",
      "  public class TestClass2 {",
      "",
      "    @Bean(name = \"value3\")",
      "    public List<String> value3() { return new ArrayList<String>();}",
      "",
      "}");
  
  
  private JavaFileObject definitionClassExpectingComplex = JavaFileObjects.forSourceLines(
      "test.TestClass1",
      "package test;",
      "",
      "import org.springframework.beans.factory.annotation.Qualifier;",
      "import org.springframework.context.annotation.Bean;",
      "import org.springframework.context.annotation.ComponentScan;",
      "import org.springframework.context.annotation.Configuration;",
      "import org.springframework.context.annotation.Import;",
      "import java.util.Map;",
      "",
      "  @com.salesforce.aptspring.Verified()",
      "  @Configuration",
      "  @Import(TestClass2.class)",
      "  public class TestClass1 {",
      "",
      "    @Bean(name = \"value1\")",
      "    public String value1(@Qualifier(\"value2\") String x) { return \"\";}",
      "",
      "    @Bean(name = \"value2\")",
      "    public String value2(@Qualifier(\"value3\") Map<String, ? extends Number> y) { return \"\";}",
      "",
      "    @Bean(name = \"value5\")",
      "    public String value2(@Qualifier(\"value1\") String y, @Qualifier(\"value2\") String z) { return \"\";}",
      "",
      "    @Bean(name = \"valueX\")",
      "    public String valueX() { return \"\";}",
      "",
      "}");
  
  JavaFileObject definitionClass2Complex = JavaFileObjects.forSourceLines(
      "test.TestClass2",
      "package test;",
      "",
      "import org.springframework.beans.factory.annotation.Qualifier;",
      "import org.springframework.context.annotation.Bean;",
      "import org.springframework.context.annotation.ComponentScan;",
      "import org.springframework.context.annotation.Configuration;",
      "import org.springframework.context.annotation.Import;",
      "import java.util.HashMap;",
      "import java.util.Map;",
      "import java.util.ArrayList;",
      "",
      "  @com.salesforce.aptspring.Verified",
      "  @Configuration",
      "  public class TestClass2 {",
      "",
      "    @Bean(name = \"value3\")",
      "    public  Map<String, Integer> value3() { return new HashMap<String, Integer>();}",
      "",
      "}");
  
  JavaFileObject definitionClass2ComplexExpecting = JavaFileObjects.forSourceLines(
      "test.TestClass2",
      "package test;",
      "",
      "import org.springframework.beans.factory.annotation.Qualifier;",
      "import org.springframework.context.annotation.Bean;",
      "import org.springframework.context.annotation.ComponentScan;",
      "import org.springframework.context.annotation.Configuration;",
      "import org.springframework.context.annotation.Import;",
      "import java.util.HashMap;",
      "import java.util.Map;",
      "import java.util.ArrayList;",
      "",
      "  @com.salesforce.aptspring.Verified(expectedBeans=\"valueX\")",
      "  @Configuration",
      "  public class TestClass2 {",
      "",
      "    @Bean(name = \"value3\")",
      "    public  Map<String, Integer> value3(@Qualifier(\"valueX\") Integer valuex) { return new HashMap<String, Integer>();}",
      "",
      "}");
  
  @Test
  public void testTypesMatchExactly() throws IOException {
    assertAbout(javaSources())
            .that(Arrays.asList(definitionClassExpectingListString, definitionClass2ProvidesListString))
            .processedWith(new VerifiedSpringConfiguration())
            .compilesWithoutError();
  }
  
  @Test
  public void testTypesMisMatchParameter() throws IOException {
    assertAbout(javaSources())
            .that(Arrays.asList(definitionClassExpectingListInteger, definitionClass2ProvidesListString))
            .processedWith(new VerifiedSpringConfiguration())
            .failsToCompile()
            .withErrorContaining("Unmatched types value3 found in test.TestClass2.value3(...)")
            .in(definitionClassExpectingListInteger)
            .onLine(19)
            .and()
            .withErrorContaining("Unmatched types value2 found in test.TestClass1.value2(...)")
            .in(definitionClass2ProvidesListString)
            .onLine(16);
  }
  
  
  @Test
  public void testTypesMisMatchExpectedParameter() throws IOException {
    assertAbout(javaSources())
            .that(Arrays.asList(definitionClassExpectingComplex, definitionClass2ComplexExpecting))
            .processedWith(new VerifiedSpringConfiguration())
            .failsToCompile()
            .withErrorContaining("Unmatched types valueX found in test.TestClass1.valueX(...),"
                + " value3 found in test.TestClass2.value3(...)")
            .in(definitionClassExpectingComplex)
            .onLine(13);
  }
  
  @Test
  public void testTypesMatchingComplex() throws IOException {
    assertAbout(javaSources())
            .that(Arrays.asList(definitionClassExpectingComplex, definitionClass2Complex))
            .processedWith(new VerifiedSpringConfiguration())
            .compilesWithoutError();
  }

}
