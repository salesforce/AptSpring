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

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import javax.tools.JavaFileObject;

import org.junit.Test;

import com.google.common.io.Files;
import com.google.testing.compile.JavaFileObjects;

public class ComponentTests {

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
      "  @Import(TestClass2.class)",
      "  public class TestClass1 {",
      "",
      "    @Bean(name = \"value1\")",
      "    public String value1(@Qualifier(\"value2\") String x) { return \"\";}",
      "",
      "    @Bean(name = \"value2\")",
      "    public String value2(@Qualifier(\"value3\") TestClass2 y) { return \"\";}",
      "",
      "}");
  
  private JavaFileObject definitionClassBreaksCycle = JavaFileObjects.forSourceLines(
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
      "    public String value1() { return \"\";}",
      "",
      "    @Bean(name = \"value2\")",
      "    public String value2(@Qualifier(\"value3\") TestClass2 y) { return \"\";}",
      "",
      "}");
  
  private JavaFileObject componentClassNoDeps = JavaFileObjects.forSourceLines(
      "test.TestClass2",
      "package test;",
      "",
      "import org.springframework.beans.factory.annotation.Qualifier;",
      "import org.springframework.context.annotation.Bean;",
      "import org.springframework.stereotype.Component;",
      "",
      "  @com.salesforce.aptspring.Verified",
      "  @Component(\"value3\")",
      "  public class TestClass2 {",
      "",
      "    public TestClass2() { }",
      "",
      "}");
  
  private JavaFileObject componentClassTwoConstructorsBusted = JavaFileObjects.forSourceLines(
      "test.TestClass2",
      "package test;",
      "",
      "import org.springframework.beans.factory.annotation.Qualifier;",
      "import org.springframework.context.annotation.Bean;",
      "import org.springframework.stereotype.Component;",
      "",
      "  @com.salesforce.aptspring.Verified",
      "  @Component(\"value3\")",
      "  public class TestClass2 {",
      "",
      "    public TestClass2() { }",
      "",
      "    public TestClass2(String bob) { }",
      "",
      "",
      "}");
  
  
  private JavaFileObject componentClassTwoConstructorsNoDeps = JavaFileObjects.forSourceLines(
      "test.TestClass2",
      "package test;",
      "",
      "import org.springframework.beans.factory.annotation.Qualifier;",
      "import org.springframework.context.annotation.Bean;",
      "import org.springframework.stereotype.Component;",
      "import org.springframework.beans.factory.annotation.Autowired;",
      "",
      "  @com.salesforce.aptspring.Verified",
      "  @Component(\"value3\")",
      "  public class TestClass2 {",
      "",
      "    @Autowired",
      "    public TestClass2() { }",
      "",
      "    public TestClass2(String bob) { }",
      "",
      "",
      "}");
  
  
  private JavaFileObject componentClassTwoConstructorsWithMissingDeps = JavaFileObjects.forSourceLines(
      "test.TestClass2",
      "package test;",
      "",
      "import org.springframework.beans.factory.annotation.Qualifier;",
      "import org.springframework.context.annotation.Bean;",
      "import org.springframework.stereotype.Component;",
      "import org.springframework.beans.factory.annotation.Autowired;",
      "",
      "  @com.salesforce.aptspring.Verified",
      "  @Component(\"value3\")",
      "  public class TestClass2 {",
      "",
      "    public TestClass2() { }",
      "",
      "    @Autowired",
      "    public TestClass2(@Qualifier(\"bob\") String bob) { }",
      "",
      "",
      "}");
  
  private JavaFileObject componentClassTwoConstructorsWithCircularDeps = JavaFileObjects.forSourceLines(
      "test.TestClass2",
      "package test;",
      "",
      "import org.springframework.beans.factory.annotation.Qualifier;",
      "import org.springframework.context.annotation.Bean;",
      "import org.springframework.stereotype.Component;",
      "import org.springframework.beans.factory.annotation.Autowired;",
      "",
      "  @com.salesforce.aptspring.Verified",
      "  @Component(\"value3\")",
      "  public class TestClass2 {",
      "",
      "    public TestClass2() { }",
      "",
      "    @Autowired",
      "    public TestClass2(@Qualifier(\"value1\") String bob) { }",
      "",
      "",
      "}");
  
  private JavaFileObject componentClassBadField = JavaFileObjects.forSourceLines(
      "test.TestClass2",
      "package test;",
      "",
      "import org.springframework.beans.factory.annotation.Qualifier;",
      "import org.springframework.context.annotation.Bean;",
      "import org.springframework.stereotype.Component;",
      "",
      "  @com.salesforce.aptspring.Verified",
      "  @Component(\"value3\")",
      "  public class TestClass2 {",
      "",
      "  private static final String COOL = \"cool\";",
      "",
      "  private final String mode;",
      "",
      "  public String mode1;",
      "",
      "  private volatile String mode2;",
      "",
      "    public TestClass2() { mode = \"x\"; }",
      "",
      "}");
  
  
  private JavaFileObject componentNoName = JavaFileObjects.forSourceLines(
      "test.TestClass1",
      "package test;",
      "",
      "import org.springframework.stereotype.Component;",
      "",
      "@com.salesforce.aptspring.Verified",
      "@Component",
      "public class TestClass1 {",
      "",
      "}");
  
  
  @Test
  public void testComponentNo() throws IOException {
    File outputDir = Files.createTempDir();
    System.out.println("Generating into " + outputDir.getAbsolutePath());

    assertAbout(javaSources())
            .that(Arrays.asList(componentNoName))
            .processedWith(new VerifiedSpringConfiguration())
            .failsToCompile()
            .withErrorContaining("@Component classes must have a name")
            .in(componentNoName)
            .onLine(7);  
  }
  
  
  @Test
  public void testComponentImport() throws IOException {
    File outputDir = Files.createTempDir();
    System.out.println("Generating into " + outputDir.getAbsolutePath());

    assertAbout(javaSources())
            .that(Arrays.asList(definitionClass, componentClassNoDeps))
            .processedWith(new VerifiedSpringConfiguration())
            .compilesWithoutError();  
  }
  
  @Test
  public void testComponentImportTwoConstructorsNoAutowired() throws IOException {
    File outputDir = Files.createTempDir();
    System.out.println("Generating into " + outputDir.getAbsolutePath());

    assertAbout(javaSources())
            .that(Arrays.asList(definitionClass, componentClassTwoConstructorsBusted))
            .processedWith(new VerifiedSpringConfiguration())
            .failsToCompile()
            .withErrorContaining("No single default constructor or single @Autowired constructor")
            .in(componentClassTwoConstructorsBusted)
            .onLine(9);
  }
  
  @Test
  public void testComponentImportTwoConstructorsAutowiredNoDeps() throws IOException {
    File outputDir = Files.createTempDir();
    System.out.println("Generating into " + outputDir.getAbsolutePath());

    assertAbout(javaSources())
            .that(Arrays.asList(definitionClass, componentClassTwoConstructorsNoDeps))
            .processedWith(new VerifiedSpringConfiguration())
            .compilesWithoutError();
  }
  
  @Test
  public void testComponentImportTwoConstructorsAutowiredWithUnsatisfiedDeps() throws IOException {
    File outputDir = Files.createTempDir();
    System.out.println("Generating into " + outputDir.getAbsolutePath());

    assertAbout(javaSources())
            .that(Arrays.asList(definitionClass, componentClassTwoConstructorsWithMissingDeps))
            .processedWith(new VerifiedSpringConfiguration())
            .failsToCompile()
            .withErrorContaining("Missing bean definitions for spring beans bob,"
                + " create definitions or list them in @Verified's expected field")
            .in(definitionClass)
            .onLine(11)
            .and()
            .withErrorContaining("Missing bean definitions for spring beans bob,"
                + " create definitions or list them in @Verified's expected field")
            .in(componentClassTwoConstructorsWithMissingDeps)
            .onLine(15);
  }
  
  @Test
  public void testComponentImportTwoConstructorsAutowiredWithCircularDeps() throws IOException {
    File outputDir = Files.createTempDir();
    System.out.println("Generating into " + outputDir.getAbsolutePath());
    assertAbout(javaSources())
            .that(Arrays.asList(definitionClass, componentClassTwoConstructorsWithCircularDeps))
            .processedWith(new VerifiedSpringConfiguration())
            .failsToCompile()
            .withErrorContaining("Cycle in @Imports")
            .in(definitionClass)
            .onLine(14)
            .and()
            .withErrorContaining("Cycle in @Imports")
            .in(definitionClass)
            .onLine(17)
            .and()
            .withErrorContaining("Cycle in @Imports")
            .in(componentClassTwoConstructorsWithCircularDeps)
            .onLine(15);
    
  }
  
  @Test
  public void testComponentImportTwoConstructorsAutowired() throws IOException {
    File outputDir = Files.createTempDir();
    System.out.println("Generating into " + outputDir.getAbsolutePath());
    assertAbout(javaSources())
            .that(Arrays.asList(definitionClassBreaksCycle, componentClassTwoConstructorsWithCircularDeps))
            .processedWith(new VerifiedSpringConfiguration())
            .compilesWithoutError();
  }

  
  @Test
  public void testComponentFields() throws IOException {
    File outputDir = Files.createTempDir();
    System.out.println("Generating into " + outputDir.getAbsolutePath());
    assertAbout(javaSources())
            .that(Arrays.asList(componentClassBadField))
            .processedWith(new VerifiedSpringConfiguration())
            .failsToCompile()
            .withErrorContaining("@Component classes my only have static final constant fields or final private fields")
            .in(componentClassBadField)
            .onLine(15)
            .and()
            .withErrorContaining("@Component classes my only have static final constant fields or final private fields")
            .in(componentClassBadField)
            .onLine(15);
  }

}
