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
/*
  * Copyright © 2016, Saleforce.com
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

public class BeanTests {

  @Test
  public void testSimpleCycle() throws IOException {
    JavaFileObject definitionClass = JavaFileObjects.forSourceLines(
            "test.TestClass",
            "package test;",
            "",
            "import org.springframework.beans.factory.annotation.Qualifier;",
            "import org.springframework.context.annotation.Bean;",
            "import org.springframework.context.annotation.ComponentScan;",
            "import org.springframework.context.annotation.Configuration;",
            "",
            "  @com.salesforce.aptspring.Verified",
            "  public class TestClass {",
            "",
            "    @Bean(name = \"value1\")",
            "    public String value1(@Qualifier(\"value2\") String x) { return \"\";}",
            "",
            "    @Bean(name = \"value2\")",
            "    public String value2(@Qualifier(\"value1\") String y) { return \"\";}",
            "",
            "}");
    
    assertAbout(javaSources())
            .that(Arrays.asList(definitionClass))
            .processedWith(new VerifiedSpringConfiguration())
            .failsToCompile()
            .withErrorContaining("Cycle in @Imports value1 found in test.TestClass.value1(java.lang.String)"
                + " -> value2 found in test.TestClass.value2(java.lang.String)")
            .in(definitionClass).onLine(12)
            .and()
            .withErrorContaining("Cycle in @Imports value2 found in test.TestClass.value2(java.lang.String)"
                + " -> value1 found in test.TestClass.value1(java.lang.String)")
            .in(definitionClass).onLine(15);
  }

  @Test
  public void testDuplicateBeans() throws IOException {
    JavaFileObject definitionClass = JavaFileObjects.forSourceLines(
            "test.TestClass",
            "package test;",
            "",
            "import org.springframework.beans.factory.annotation.Qualifier;",
            "import org.springframework.context.annotation.Bean;",
            "import org.springframework.context.annotation.ComponentScan;",
            "import org.springframework.context.annotation.Configuration;",
            "",
            "  @com.salesforce.aptspring.Verified",
            "  public class TestClass {",
            "",
            "    @Bean(name = \"value1\")",
            "    public String value1() { return \"\";}",
            "",
            "    @Bean(name = \"value1\")",
            "    public String value2() { return \"\";}",
            "",
            "}");
    
    assertAbout(javaSources())
            .that(Arrays.asList(definitionClass))
            .processedWith(new VerifiedSpringConfiguration())
            .failsToCompile()
            .withErrorContaining("Duplicate in spring beans value1 found in test.TestClass.value2()")
            .in(definitionClass).onLine(12)
            .and()
            .withErrorContaining("Duplicate in spring beans value1 found in test.TestClass.value1()")
            .in(definitionClass).onLine(15);
  }

  @Test
  public void testMissingBeans() throws IOException {
    JavaFileObject definitionClass = JavaFileObjects.forSourceLines(
        "test.TestClass2",
        "package test;",
        "",
        "import org.springframework.beans.factory.annotation.Qualifier;",
        "import org.springframework.context.annotation.Bean;",
        "import org.springframework.context.annotation.ComponentScan;",
        "import org.springframework.context.annotation.Configuration;",
        "import org.springframework.context.annotation.Import;",
        "",
        "  @com.salesforce.aptspring.Verified()",
        "  public class TestClass2 {",
        "",
        "    @Bean(name = \"value3\")",
        "    public String value3(@Qualifier(\"value1\") String x) { return \"\";}",
        "",
        "    @Bean(name = \"value4\")",
        "    public String value4() { return \"\";}",
        "",
        "}");

    assertAbout(javaSources())
            .that(Arrays.asList(definitionClass))
            .processedWith(new VerifiedSpringConfiguration())
            .failsToCompile()
            .withErrorContaining("Missing bean definitions for spring beans value1,"
                + " create definitions or list them in @Verified's expected field")
            .in(definitionClass)
            .onLine(10)
            .and()
            .withErrorContaining("Missing bean definitions for spring beans value1,"
                + " create definitions or list them in @Verified's expected field")
            .in(definitionClass)
            .onLine(13);
  }
  
  
  @Test
  public void testBreakMemberVariablesInConfiguration() throws IOException {
    JavaFileObject definitionClass = JavaFileObjects.forSourceLines(
        "test.TestClass2",
        "package test;",
        "",
        "import org.springframework.beans.factory.annotation.Qualifier;",
        "import org.springframework.context.annotation.Bean;",
        "import org.springframework.context.annotation.ComponentScan;",
        "import org.springframework.context.annotation.Configuration;",
        "import org.springframework.context.annotation.Import;",
        "",
        "  @com.salesforce.aptspring.Verified(expectedBeans=\"{value1}\")",
        "  public class TestClass2 {",
        "",
        "    private static String someVariable;",
        "",
        "    @Bean(name = \"value4\")",
        "    public String value4() { return \"\";}",
        "",
        "}");

    assertAbout(javaSources())
            .that(Arrays.asList(definitionClass))
            .processedWith(new VerifiedSpringConfiguration())
            .failsToCompile()
            .withErrorContaining("Only private static final constants are permitted in @Verified @Configuration classes")
            .in(definitionClass)
            .onLine(12);
  }
  
  
  @Test
  public void testMissingBeansThatAreExpected() throws IOException {
    JavaFileObject definitionClass = JavaFileObjects.forSourceLines(
        "test.TestClass2",
        "package test;",
        "",
        "import org.springframework.beans.factory.annotation.Qualifier;",
        "import org.springframework.context.annotation.Bean;",
        "import org.springframework.context.annotation.ComponentScan;",
        "import org.springframework.context.annotation.Configuration;",
        "import org.springframework.context.annotation.Import;",
        "",
        "  @com.salesforce.aptspring.Verified(expectedBeans={\"value1\"})",
        "  public class TestClass2 {",
        "",
        "    @Bean(name = \"value3\")",
        "    public String value3(@Qualifier(\"value1\") String x) { return \"\";}",
        "",
        "    @Bean(name = \"value4\")",
        "    public String value4() { return \"\";}",
        "",
        "}");

    assertAbout(javaSources())
            .that(Arrays.asList(definitionClass))
            .processedWith(new VerifiedSpringConfiguration())
            .compilesWithoutError();
  }

}
