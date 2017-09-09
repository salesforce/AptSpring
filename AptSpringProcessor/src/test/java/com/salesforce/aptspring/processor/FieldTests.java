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

public class FieldTests {

  @Test
  public void testFieldIsAllGood() throws IOException {
    JavaFileObject definitionClass = JavaFileObjects.forSourceLines(
        "test.TestClass",
        "package test;",
        "",
        "import org.springframework.context.annotation.Configuration;",
        "import java.util.Date;",
        "",
        "  @com.salesforce.aptspring.Verified",
        "  @Configuration",
        "  public class TestClass {",
        "",
        "    private static final String someVariable = \"I AM GOOD!\";",
        "",
        "}");

    assertAbout(javaSources())
            .that(Arrays.asList(definitionClass))
            .processedWith(new VerifiedSpringConfiguration())
            .compilesWithoutError();
  }
  
  @Test
  public void testFieldDoesntHaveConstantValue() throws IOException {
    JavaFileObject definitionClass = JavaFileObjects.forSourceLines(
        "test.TestClass",
        "package test;",
        "",
        "import org.springframework.context.annotation.Configuration;",
        "",
        "  @com.salesforce.aptspring.Verified",
        "  @Configuration",
        "  public class TestClass {",
        "",
        "    private static final String someVariable = null;",
        "",
        "}");

    assertAbout(javaSources())
            .that(Arrays.asList(definitionClass))
            .processedWith(new VerifiedSpringConfiguration())
            .failsToCompile()
            .withErrorContaining("Only private static final constants are permitted in @Verified @Configuration classes")
            .in(definitionClass)
            .onLine(9);
  }
  
  @Test
  public void testFieldIsntStatic() throws IOException {
    JavaFileObject definitionClass = JavaFileObjects.forSourceLines(
        "test.TestClass",
        "package test;",
        "",
        "import org.springframework.context.annotation.Configuration;",
        "",
        "  @com.salesforce.aptspring.Verified",
        "  @Configuration",
        "  public class TestClass {",
        "",
        "    private final String someVariable = \"x\";",
        "",
        "}");

    assertAbout(javaSources())
            .that(Arrays.asList(definitionClass))
            .processedWith(new VerifiedSpringConfiguration())
            .failsToCompile()
            .withErrorContaining("Only private static final constants are permitted in @Verified @Configuration classes")
            .in(definitionClass)
            .onLine(9);
  }
  
  @Test
  public void testFieldIsntFinal() throws IOException {
    JavaFileObject definitionClass = JavaFileObjects.forSourceLines(
        "test.TestClass",
        "package test;",
        "",
        "import org.springframework.context.annotation.Configuration;",
        "",
        "  @com.salesforce.aptspring.Verified",
        "  @Configuration",
        "  public class TestClass {",
        "",
        "    private static String someVariable = \"x\";",
        "",
        "}");

    assertAbout(javaSources())
            .that(Arrays.asList(definitionClass))
            .processedWith(new VerifiedSpringConfiguration())
            .failsToCompile()
            .withErrorContaining("Only private static final constants are permitted in @Verified @Configuration classes")
            .in(definitionClass)
            .onLine(9);
  }
  
  @Test
  public void testFieldIsntPrivate() throws IOException {
    JavaFileObject definitionClass = JavaFileObjects.forSourceLines(
        "test.TestClass",
        "package test;",
        "",
        "import org.springframework.context.annotation.Configuration;",
        "",
        "  @com.salesforce.aptspring.Verified",
        "  @Configuration",
        "  public class TestClass {",
        "",
        "    static final String someVariable = \"x\";",
        "",
        "}");

    assertAbout(javaSources())
            .that(Arrays.asList(definitionClass))
            .processedWith(new VerifiedSpringConfiguration())
            .failsToCompile()
            .withErrorContaining("Only private static final constants are permitted in @Verified @Configuration classes")
            .in(definitionClass)
            .onLine(9);
  }
  
  @Test
  public void testFieldIsntALiteral() throws IOException {
    JavaFileObject definitionClass = JavaFileObjects.forSourceLines(
        "test.TestClass",
        "package test;",
        "",
        "import org.springframework.context.annotation.Configuration;",
        "import java.util.Date;",
        "",
        "  @com.salesforce.aptspring.Verified",
        "  @Configuration",
        "  public class TestClass {",
        "",
        "    private static final Date someVariable = new Date();",
        "",
        "}");

    assertAbout(javaSources())
            .that(Arrays.asList(definitionClass))
            .processedWith(new VerifiedSpringConfiguration())
            .failsToCompile()
            .withErrorContaining("Only private static final constants are permitted in @Verified @Configuration classes")
            .in(definitionClass)
            .onLine(10);
  }
  
  @Test
  public void testHasNoModifiers() throws IOException {
    JavaFileObject definitionClass = JavaFileObjects.forSourceLines(
        "test.TestClass",
        "package test;",
        "",
        "import org.springframework.context.annotation.Configuration;",
        "import java.util.Date;",
        "",
        "  @com.salesforce.aptspring.Verified",
        "  @Configuration",
        "  public class TestClass {",
        "",
        "    String someVariable = \"I AM GOOD!\";",
        "",
        "}");

    assertAbout(javaSources())
            .that(Arrays.asList(definitionClass))
            .processedWith(new VerifiedSpringConfiguration())
            .failsToCompile()
            .withErrorContaining("Only private static final constants are permitted in @Verified @Configuration classes")
            .in(definitionClass)
            .onLine(10);
  }
}
