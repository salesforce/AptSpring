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

public class ParserTests {

  @Test
  public void testFieldIsAllGood() throws IOException {
    JavaFileObject definitionClass = JavaFileObjects.forSourceLines(
        "test.TestClass",
        "package test;",
        "",
        "import org.springframework.context.annotation.Configuration;",
        "import java.util.Date;",
        "",
        "public class TestClass {",
        "",
        "  @com.salesforce.aptspring.Verified",
        "  @Configuration",
        "  public static class TestClass1 {",
        "",
        "    private static final String someVariable = \"I AM GOOD!\";",
        "  }",
        "",
        "}");

    assertAbout(javaSources())
            .that(Arrays.asList(definitionClass))
            .processedWith(new VerifiedSpringConfiguration())
            .failsToCompile()
            .withErrorContaining("The class must be a top level class, not an internal class")
            .in(definitionClass)
            .onLine(10);
  }
  
  @Test
  public void testConfigurationBeanMethodMustHaveAtLeastOneName() throws IOException {
    JavaFileObject definitionClass = JavaFileObjects.forSourceLines(
        "test.TestClass",
        "package test;",
        "",
        "import org.springframework.context.annotation.Bean;",
        "import org.springframework.context.annotation.Configuration;",
        "",
        "@com.salesforce.aptspring.Verified",
        "@Configuration",
        "public class TestClass {",
        "",
        "  @Bean",
        "  private String getValue() { return \"I AM NOT GOOD!\"; }",
        "",
        "}");

    assertAbout(javaSources())
            .that(Arrays.asList(definitionClass))
            .processedWith(new VerifiedSpringConfiguration())
            .failsToCompile()
            .withErrorContaining("All @Bean annotations must define at least one name for a bean")
            .in(definitionClass)
            .onLine(11);
  }

  @Test
  public void testConfigurationBeanMethodMayHaveManyNames() throws IOException {
    JavaFileObject definitionClass = JavaFileObjects.forSourceLines(
        "test.TestClass",
        "package test;",
        "",
        "import org.springframework.context.annotation.Bean;",
        "import org.springframework.context.annotation.Configuration;",
        "",
        "@com.salesforce.aptspring.Verified",
        "@Configuration",
        "public class TestClass {",
        "",
        "  @Bean(name={\"name1\",\"name2\"})",
        "  public String getValue() { return \"I AM NOT GOOD!\"; }",
        "",
        "}");

    assertAbout(javaSources())
            .that(Arrays.asList(definitionClass))
            .processedWith(new VerifiedSpringConfiguration())
            .compilesWithoutError();
  }
  
  @Test
  public void testMustHaveConfiguration() throws IOException {
    JavaFileObject definitionClass = JavaFileObjects.forSourceLines(
        "test.TestClass",
        "package test;",
        "",
        "import org.springframework.context.annotation.Configuration;",
        "",
        "@com.salesforce.aptspring.Verified",
        "public class TestClass {",
        "",
        "  private static final String someVariable = \"I AM GOOD!\";",
        "",
        "}");

    assertAbout(javaSources())
            .that(Arrays.asList(definitionClass))
            .processedWith(new VerifiedSpringConfiguration())
            .failsToCompile()
            .withErrorContaining("@Verified annotation must only be used on @Configuration classes")
            .in(definitionClass)
            .onLine(6);
  }
  
  
  @Test
  public void testConfigurationMethodMustHaveBean() throws IOException {
    JavaFileObject definitionClass = JavaFileObjects.forSourceLines(
        "test.TestClass",
        "package test;",
        "",
        "import org.springframework.context.annotation.Configuration;",
        "",
        "@com.salesforce.aptspring.Verified",
        "@Configuration",
        "public class TestClass {",
        "",
        "  private String getValue() { return \"I AM NOT GOOD!\"; }",
        "",
        "}");

    assertAbout(javaSources())
            .that(Arrays.asList(definitionClass))
            .processedWith(new VerifiedSpringConfiguration())
            .failsToCompile()
            .withErrorContaining("All methods on @Configuration must have @Bean annotation")
            .in(definitionClass)
            .onLine(9);
  }
  
  @Test
  public void testConfigurationBeanMethodMustBePublic() throws IOException {
    JavaFileObject definitionClass = JavaFileObjects.forSourceLines(
        "test.TestClass",
        "package test;",
        "",
        "import org.springframework.context.annotation.Bean;",
        "import org.springframework.context.annotation.Configuration;",
        "",
        "@com.salesforce.aptspring.Verified",
        "@Configuration",
        "public class TestClass {",
        "",
        "  @Bean(name = {\"stuff\",\"stuff2\"})",
        "  private String getValue() { return \"I AM NOT GOOD!\"; }",
        "",
        "}");

    assertAbout(javaSources())
            .that(Arrays.asList(definitionClass))
            .processedWith(new VerifiedSpringConfiguration())
            .failsToCompile()
            .withErrorContaining("@Bean methods must be marked public")
            .in(definitionClass)
            .onLine(11);
  }
  
  @Test
  public void testConfigurationBeanMustReturnAnObject() throws IOException {
    JavaFileObject definitionClass = JavaFileObjects.forSourceLines(
        "test.TestClass",
        "package test;",
        "",
        "import org.springframework.context.annotation.Bean;",
        "import org.springframework.context.annotation.Configuration;",
        "",
        "@com.salesforce.aptspring.Verified",
        "@Configuration",
        "public class TestClass {",
        "",
        "  @Bean(name=\"stuff\")",
        "  public void getValue() { return \"I AM NOT GOOD!\"; }",
        "",
        "}");

    assertAbout(javaSources())
            .that(Arrays.asList(definitionClass))
            .processedWith(new VerifiedSpringConfiguration())
            .failsToCompile()
            .withErrorContaining("@Bean methods must return an Object")
            .in(definitionClass)
            .onLine(11);
  }
  
  @Test
  public void testConfigurationBeanMustReturnAnObjectNotInt() throws IOException {
    JavaFileObject definitionClass = JavaFileObjects.forSourceLines(
        "test.TestClass",
        "package test;",
        "",
        "import org.springframework.context.annotation.Bean;",
        "import org.springframework.context.annotation.Configuration;",
        "",
        "@com.salesforce.aptspring.Verified",
        "@Configuration",
        "public class TestClass {",
        "",
        "  @Bean(name=\"stuff\")",
        "  public int getValue() { return 1; }",
        "",
        "}");

    assertAbout(javaSources())
            .that(Arrays.asList(definitionClass))
            .processedWith(new VerifiedSpringConfiguration())
            .failsToCompile()
            .withErrorContaining("@Bean methods must return an Object")
            .in(definitionClass)
            .onLine(11);
  }
  
  @Test
  public void testConfigurationBeanMayNotBeStatic() throws IOException {
    JavaFileObject definitionClass = JavaFileObjects.forSourceLines(
        "test.TestClass",
        "package test;",
        "",
        "import org.springframework.context.annotation.Bean;",
        "import org.springframework.context.annotation.Configuration;",
        "",
        "@com.salesforce.aptspring.Verified",
        "@Configuration",
        "public class TestClass {",
        "",
        "  @Bean(name=\"stuff\")",
        "  public static String getValue() { return \"\"; }",
        "",
        "}");

    assertAbout(javaSources())
            .that(Arrays.asList(definitionClass))
            .processedWith(new VerifiedSpringConfiguration())
            .failsToCompile()
            .withErrorContaining("Illegal modifiers found on spring @Bean method: STATIC")
            .in(definitionClass)
            .onLine(11);
  }
  
  @Test
  public void testConfigurationMayNotHaveConstructorsOrInitializationBlocks() throws IOException {
    JavaFileObject definitionClass = JavaFileObjects.forSourceLines(
        "test.TestClass",
        "package test;",
        "",
        "import org.springframework.context.annotation.Bean;",
        "import org.springframework.context.annotation.Configuration;",
        "",
        "@com.salesforce.aptspring.Verified",
        "@Configuration",
        "public class TestClass {",
        "",
        " public TestClass(String s) {",
        "   String s2 = s;",
        " }",
        "",
        "}");

    assertAbout(javaSources())
            .that(Arrays.asList(definitionClass))
            .processedWith(new VerifiedSpringConfiguration())
            .failsToCompile()
            .withErrorContaining("@Configuration should not have any non-defualt constructors.")
            .in(definitionClass)
            .onLine(10);
  }
  
  @Test
  public void testConfigurationMayNotHaveNonPublicConstructors() throws IOException {
    JavaFileObject definitionClass = JavaFileObjects.forSourceLines(
        "test.TestClass",
        "package test;",
        "",
        "import org.springframework.context.annotation.Bean;",
        "import org.springframework.context.annotation.Configuration;",
        "",
        "@com.salesforce.aptspring.Verified",
        "@Configuration",
        "public class TestClass {",
        "",
        " private TestClass() {",
        " }",
        "",
        "}");

    assertAbout(javaSources())
            .that(Arrays.asList(definitionClass))
            .processedWith(new VerifiedSpringConfiguration())
            .failsToCompile()
            .withErrorContaining("@Configuration should not have any non-public constructors.")
            .in(definitionClass)
            .onLine(10);
  }
  
  @Test
  public void testConfigurationErrorIfComponentScan() throws IOException {
    JavaFileObject definitionClass = JavaFileObjects.forSourceLines(
        "test.TestClass",
        "package test;",
        "",
        "import org.springframework.context.annotation.Bean;",
        "import org.springframework.context.annotation.Configuration;",
        "import org.springframework.context.annotation.ComponentScan;",
        "",
        "@com.salesforce.aptspring.Verified",
        "@Configuration",
        "@ComponentScan(\"stuff1\")",
        "public class TestClass {",
        "",
        "  @Bean(name=\"stuff\")",
        "  public String getValue() { return \"\"; }",
        "",
        "}");

    assertAbout(javaSources())
            .that(Arrays.asList(definitionClass))
            .processedWith(new VerifiedSpringConfiguration())
            .failsToCompile()
            .withErrorContaining("You may not use @ComponentScan(s) on @Verified classes")
            .in(definitionClass)
            .onLine(10);
  }
  
  @Test
  public void testConfigurationErrorIfComponentScans() throws IOException {
    JavaFileObject definitionClass = JavaFileObjects.forSourceLines(
        "test.TestClass",
        "package test;",
        "",
        "import org.springframework.context.annotation.Bean;",
        "import org.springframework.context.annotation.Configuration;",
        "import org.springframework.context.annotation.ComponentScan;",
        "",
        "@com.salesforce.aptspring.Verified",
        "@Configuration",
        "@ComponentScan(\"stuff1\")",
        "@ComponentScan(\"stuff2\")",
        "public class TestClass {",
        "",
        "  @Bean(name=\"stuff\")",
        "  public String getValue() { return \"\"; }",
        "",
        "}");

    assertAbout(javaSources())
            .that(Arrays.asList(definitionClass))
            .processedWith(new VerifiedSpringConfiguration())
            .failsToCompile()
            .withErrorContaining("You may not use @ComponentScan(s) on @Verified classes")
            .in(definitionClass)
            .onLine(11);
  }
  
  
  @Test
  public void testFailIfUnmarkedParameter() throws IOException {
    JavaFileObject definitionClass = JavaFileObjects.forSourceLines(
        "test.TestClass",
        "package test;",
        "",
        "import org.springframework.context.annotation.Bean;",
        "import org.springframework.context.annotation.Configuration;",
        "import org.springframework.context.annotation.ComponentScan;",
        "import org.springframework.beans.factory.annotation.Value;",
        "import org.springframework.beans.factory.annotation.Qualifier;",
        "",
        "@com.salesforce.aptspring.Verified",
        "@Configuration",
        "public class TestClass {",
        "",
        "  @Bean(value=\"stuff1\")",
        "  public String getValue1(@Value(\"x\") String s) { return \"\"; }",
        " ",
        "  @Bean(value=\"stuff2\")",
        "  public String getValue2(@Qualifier(\"bob\") String b) { return \"\"; }",
        " ",
        "  @Bean(name=\"stuff3\")",
        "  public String getValue3(String c) { return \"\"; }",
        " ",
        "}");

    assertAbout(javaSources())
            .that(Arrays.asList(definitionClass))
            .processedWith(new VerifiedSpringConfiguration())
            .failsToCompile()
            .withErrorContaining("All parameters must have an @Qualifier or a @Value annotation")
            .in(definitionClass)
            .onLine(20);
  }
  
  @Test
  public void testFailIfUnexpectedElement() throws IOException {
    JavaFileObject definitionClass = JavaFileObjects.forSourceLines(
        "test.TestClass",
        "package test;",
        "",
        "import org.springframework.context.annotation.Bean;",
        "import org.springframework.context.annotation.Configuration;",
        "",
        "@com.salesforce.aptspring.Verified",
        "@Configuration",
        "public class TestClass {",
        "",
        "  public enum Day { SUNDAY, MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY };",
        " ",
        "}");

    assertAbout(javaSources())
            .that(Arrays.asList(definitionClass))
            .processedWith(new VerifiedSpringConfiguration())
            .failsToCompile()
            .withErrorContaining("Only @Bean methods, private static final literals,"
                + " and default constructors are allowed on @Configuration classes")
            .in(definitionClass)
            .onLine(10);
  }

  @Test
  public void testFailUsingValueAndNotRootConfiguration() throws IOException {
    JavaFileObject definitionClass = JavaFileObjects.forSourceLines(
        "test.TestClass",
        "package test;",
        "",
        "import org.springframework.context.annotation.Bean;",
        "import org.springframework.context.annotation.Configuration;",
        "import org.springframework.context.annotation.ComponentScan;",
        "import org.springframework.beans.factory.annotation.Value;",
        "import org.springframework.beans.factory.annotation.Qualifier;",
        "",
        "@com.salesforce.aptspring.Verified",
        "@Configuration",
        "public class TestClass {",
        "",
        "  @Bean(value=\"stuff1\")",
        "  public String getValue1(@Value(\"x\") String s) { return \"\"; }",
        " ",
        "  @Bean(value=\"stuff2\")",
        "  public String getValue2(@Qualifier(\"stuff1\") String b, @Value(\"x\") String s) { return \"\"; }",
        " ",
        "}");

    assertAbout(javaSources())
            .that(Arrays.asList(definitionClass))
            .processedWith(new VerifiedSpringConfiguration())
            .failsToCompile()
            .withErrorContaining("Only @Verified(root=true) nodes may use @Value annotations to create beans,"
                + " decouples spring graph from environment")
            .in(definitionClass)
            .onLine(14)
            .and()
            .withErrorContaining("No method may define both @Qualifier or a @Value annotations,"
                + " keep property values in there own beans")
            .in(definitionClass)
            .onLine(17);
  }

  @Test
  public void testRootConfigurationCompilesWithValues() throws IOException {
    JavaFileObject definitionClass = JavaFileObjects.forSourceLines(
        "test.TestClass",
        "package test;",
        "",
        "import org.springframework.context.annotation.Bean;",
        "import org.springframework.context.annotation.Configuration;",
        "import org.springframework.context.annotation.ComponentScan;",
        "import org.springframework.beans.factory.annotation.Value;",
        "import org.springframework.beans.factory.annotation.Qualifier;",
        "",
        "@com.salesforce.aptspring.Verified(root=true)",
        "@Configuration",
        "public class TestClass {",
        " ",
        "  @Bean(value=\"stuff1\")",
        "  public String getValue1(@Value(\"x\") String s) { return \"\"; }",
        " ",
        "}");

    assertAbout(javaSources())
            .that(Arrays.asList(definitionClass))
            .processedWith(new VerifiedSpringConfiguration())
            .compilesWithoutError();
  }


}
