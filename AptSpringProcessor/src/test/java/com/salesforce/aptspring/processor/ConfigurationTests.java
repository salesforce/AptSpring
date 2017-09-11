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

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import javax.tools.JavaFileObject;

import org.junit.Test;

import com.google.common.io.Files;
import com.google.testing.compile.JavaFileObjects;

public class ConfigurationTests {

  private JavaFileObject definitionClass = JavaFileObjects.forSourceLines(
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
      "  @Configuration",
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
  
  private JavaFileObject definitionClass2CausesDefinitionCycle = JavaFileObjects.forSourceLines(
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
      "  @Configuration",
      "  @Import(TestClass1.class)",
      "  public class TestClass2 {",
      "",
      "    @Bean(name = \"value3\")",
      "    public String value3(@Qualifier(\"value4\") String x) { return \"\";}",
      "",
      "    @Bean(name = \"value4\")",
      "    public String value4() { return \"\";}",
      "",
      "}");

  
  private JavaFileObject definitionClass2NotVerified = JavaFileObjects.forSourceLines(
      "test.TestClass2",
      "package test;",
      "",
      "import org.springframework.beans.factory.annotation.Qualifier;",
      "import org.springframework.context.annotation.Bean;",
      "import org.springframework.context.annotation.ComponentScan;",
      "import org.springframework.context.annotation.Configuration;",
      "import org.springframework.context.annotation.Import;",
      "",
      "  @Configuration",
      "  public class TestClass2 {",
      "",
      "    @Bean(name = \"value3\")",
      "    public String value3(@Qualifier(\"value4\") String x) { return \"\";}",
      "",
      "    @Bean(name = \"value4\")",
      "    public String value4() { return \"\";}",
      "",
      "}");
  
  private JavaFileObject definitionClass2RootConfiguration = JavaFileObjects.forSourceLines(
      "test.TestClass2",
      "package test;",
      "",
      "import org.springframework.beans.factory.annotation.Qualifier;",
      "import org.springframework.context.annotation.Bean;",
      "import org.springframework.context.annotation.ComponentScan;",
      "import org.springframework.context.annotation.Configuration;",
      "import org.springframework.context.annotation.Import;",
      "",
      "  @com.salesforce.aptspring.Verified(root=true)",
      "  @Configuration",
      "  public class TestClass2 {",
      "",
      "    @Bean(name = \"value3\")",
      "    public String value3(@Qualifier(\"value4\") String x) { return \"\";}",
      "",
      "    @Bean(name = \"value4\")",
      "    public String value4() { return \"\";}",
      "",
      "}");
  
  @Test
  public void testCycleInImports() throws IOException {
    File outputDir = Files.createTempDir();
    System.out.println("Generating into " + outputDir.getAbsolutePath());

    assertAbout(javaSources())
            .that(Arrays.asList(definitionClass, definitionClass2CausesDefinitionCycle))
            .processedWith(new VerifiedSpringConfiguration())
            .failsToCompile()
            .withErrorContaining("Cycle in @Configuration class @Imports test.TestClass1 -> test.TestClass2")
            .in(definitionClass)
            .onLine(12)
            .and()
            .withErrorContaining("Cycle in @Configuration class @Imports test.TestClass2 -> test.TestClass1")
            .in(definitionClass2CausesDefinitionCycle)
            .onLine(12);   
  }
  
  @Test
  public void testImportNotVerified() throws IOException {
    File outputDir = Files.createTempDir();
    System.out.println("Generating into " + outputDir.getAbsolutePath());
    assertAbout(javaSources())
            .that(Arrays.asList(definitionClass, definitionClass2NotVerified))
            .processedWith(new VerifiedSpringConfiguration())
            .failsToCompile()
            .withErrorContaining("Missing @Verified or @Configuration on classes test.TestClass2")
            .in(definitionClass)
            .onLine(12);
  }

  @Test
  public void testCantImportRootConfiguration() throws IOException {
    File outputDir = Files.createTempDir();
    System.out.println("Generating into " + outputDir.getAbsolutePath());
    assertAbout(javaSources())
            .that(Arrays.asList(definitionClass, definitionClass2RootConfiguration))
            .processedWith(new VerifiedSpringConfiguration())          
            .failsToCompile()
            .withErrorContaining("@Verfied(root=true) may not be @Imported by other "
                + "@Verified @Configuration classes: test.TestClass2")
            .in(definitionClass)
            .onLine(12);
  }

  
  
  
}
