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
package com.salesforce.aptspring.processor;

import static com.google.common.truth.Truth.assertAbout;
import static com.google.testing.compile.JavaSourcesSubjectFactory.javaSources;

import java.io.IOException;
import java.util.Arrays;

import javax.tools.JavaFileObject;

import org.junit.Test;

import com.google.testing.compile.JavaFileObjects;

public class ExpectedTests {
  
  private JavaFileObject definitionClassNotExpectingValue5 = JavaFileObjects.forSourceLines(
      "test.TestClass1",
      "package test;",
      "",
      "import org.springframework.beans.factory.annotation.Qualifier;",
      "import org.springframework.context.annotation.Bean;",
      "import org.springframework.context.annotation.ComponentScan;",
      "import org.springframework.context.annotation.Configuration;",
      "import org.springframework.context.annotation.Import;",
      "",
      "  @com.salesforce.aptspring.Verified",
      "  @Import(TestClass2.class)",
      "  public class TestClass1 {",
      "",
      "    @Bean(name = \"value1\")",
      "    public String value1(@Qualifier(\"value2\") String x) { return \"\";}",
      "",
      "    @Bean(name = \"value2\")",
      "    public String value2(@Qualifier(\"value3\") String y) { return \"\";}",
      "",
      "}");
  
  private JavaFileObject definitionClassExpectingValue5 = JavaFileObjects.forSourceLines(
      "test.TestClass1",
      "package test;",
      "",
      "import org.springframework.beans.factory.annotation.Qualifier;",
      "import org.springframework.context.annotation.Bean;",
      "import org.springframework.context.annotation.ComponentScan;",
      "import org.springframework.context.annotation.Configuration;",
      "import org.springframework.context.annotation.Import;",
      "",
      "  @com.salesforce.aptspring.Verified(expectedBeans={\"value5\"})",
      "  @Import(TestClass2.class)",
      "  public class TestClass1 {",
      "",
      "    @Bean(name = \"value1\")",
      "    public String value1(@Qualifier(\"value2\") String x) { return \"\";}",
      "",
      "    @Bean(name = \"value2\")",
      "    public String value2(@Qualifier(\"value3\") String y) { return \"\";}",
      "",
      "}");
  
  private JavaFileObject definitionClassHasValue5 = JavaFileObjects.forSourceLines(
      "test.TestClass1",
      "package test;",
      "",
      "import org.springframework.beans.factory.annotation.Qualifier;",
      "import org.springframework.context.annotation.Bean;",
      "import org.springframework.context.annotation.ComponentScan;",
      "import org.springframework.context.annotation.Configuration;",
      "import org.springframework.context.annotation.Import;",
      "",
      "  @com.salesforce.aptspring.Verified",
      "  @Import(TestClass2.class)",
      "  public class TestClass1 {",
      "",
      "    @Bean(name = \"value5\")",
      "    public String value1(@Qualifier(\"value4\") String x) { return \"\";}",
      "",
      "    @Bean(name = \"value2\")",
      "    public String value2(@Qualifier(\"value3\") String y) { return \"\";}",
      "",
      "}");
  
  private JavaFileObject definitionClassHasAndExpectsValue5 = JavaFileObjects.forSourceLines(
      "test.TestClass1",
      "package test;",
      "",
      "import org.springframework.beans.factory.annotation.Qualifier;",
      "import org.springframework.context.annotation.Bean;",
      "import org.springframework.context.annotation.ComponentScan;",
      "import org.springframework.context.annotation.Configuration;",
      "import org.springframework.context.annotation.Import;",
      "",
      "  @com.salesforce.aptspring.Verified(expectedBeans={\"value5\"})",
      "  @Import(TestClass2.class)",
      "  public class TestClass1 {",
      "",
      "    @Bean(name = \"value5\")",
      "    public String value1(@Qualifier(\"value4\") String x) { return \"\";}",
      "",
      "    @Bean(name = \"value2\")",
      "    public String value2(@Qualifier(\"value3\") String y) { return \"\";}",
      "",
      "}");
  
  
  private JavaFileObject definitionClass2ExpectingValue5 = JavaFileObjects.forSourceLines(
      "test.TestClass2",
      "package test;",
      "",
      "import org.springframework.beans.factory.annotation.Qualifier;",
      "import org.springframework.context.annotation.Bean;",
      "import org.springframework.context.annotation.ComponentScan;",
      "import org.springframework.context.annotation.Configuration;",
      "import org.springframework.context.annotation.Import;",
      "",
      "  @com.salesforce.aptspring.Verified(expectedBeans={\"value5\"})",
      "  public class TestClass2 {",
      "",
      "    @Bean(name = \"value3\")",
      "    public String value3(@Qualifier(\"value5\") String x) { return \"\";}",
      "",
      "    @Bean(name = \"value4\")",
      "    public String value4() { return \"\";}",
      "",
      "}");

  
  private JavaFileObject definitionClass2MissingExpected = JavaFileObjects.forSourceLines(
      "test.TestClass2",
      "package test;",
      "",
      "import org.springframework.beans.factory.annotation.Qualifier;",
      "import org.springframework.context.annotation.Bean;",
      "import org.springframework.context.annotation.ComponentScan;",
      "import org.springframework.context.annotation.Configuration;",
      "import org.springframework.context.annotation.Import;",
      "",
      "  @com.salesforce.aptspring.Verified",
      "  public class TestClass2 {",
      "",
      "    @Bean(name = \"value3\")",
      "    public String value3(@Qualifier(\"value5\") String x) { return \"\";}",
      "",
      "    @Bean(name = \"value4\")",
      "    public String value4() { return \"\";}",
      "",
      "}");

  
  @Test
  public void testImportMissingExpected() throws IOException {
    assertAbout(javaSources())
            .that(Arrays.asList(definitionClass2MissingExpected))
            .processedWith(new VerifiedSpringConfiguration())
            .failsToCompile()
            .withErrorContaining("Missing bean definitions for spring beans value5,"
                + " create definitions or list them in @Verified's expected field")
            .in(definitionClass2MissingExpected)
            .onLine(10)
            .and()
            .withErrorContaining("Missing bean definitions for spring beans value5,"
                + " create definitions or list them in @Verified's expected field")
            .in(definitionClass2MissingExpected)
            .onLine(13);
    
  }
  
  @Test
  public void testImportExpectedPropigate() throws IOException {
    assertAbout(javaSources())
            .that(Arrays.asList(definitionClassExpectingValue5, definitionClass2ExpectingValue5))
            .processedWith(new VerifiedSpringConfiguration())
            .compilesWithoutError();
    
  }
  
  @Test
  public void testImportExpectedPropigateFailures() throws IOException {
    assertAbout(javaSources())
            .that(Arrays.asList(definitionClassNotExpectingValue5, definitionClass2ExpectingValue5))
            .processedWith(new VerifiedSpringConfiguration())
            .failsToCompile()
            .withErrorContaining("Missing bean definitions for spring beans value5,"
                + " create definitions or list them in @Verified's expected field")
            .in(definitionClassNotExpectingValue5)
            .onLine(11)
            .and()
            .withErrorContaining("Missing bean definitions for spring beans value5,"
                + " create definitions or list them in @Verified's expected field")
            .in(definitionClass2MissingExpected)
            .onLine(13);
    
  }
  
  @Test
  public void testImportWithSupplied() throws IOException {
    assertAbout(javaSources())
            .that(Arrays.asList(definitionClassHasValue5, definitionClass2ExpectingValue5))
            .processedWith(new VerifiedSpringConfiguration())
            .compilesWithoutError();
  }
  
  @Test
  public void testImportWithSuppliedAndStillExpected() throws IOException {
    assertAbout(javaSources())
            .that(Arrays.asList(definitionClassHasAndExpectsValue5, definitionClass2ExpectingValue5))
            .processedWith(new VerifiedSpringConfiguration())
            .failsToCompile()
            .withErrorContaining("Expected bean name is unnecessary value5 found in test.TestClass1")
            .in(definitionClassHasAndExpectsValue5)
            .onLine(11);
  }
  
}
