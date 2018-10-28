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
package com.salesforce.aptspring.processor.metaannotation;

import static com.google.common.truth.Truth.assertAbout;
import static com.google.testing.compile.JavaSourcesSubjectFactory.javaSources;

import java.io.IOException;
import java.util.Arrays;

import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;

import org.junit.Test;

import com.google.testing.compile.JavaFileObjects;
import com.salesforce.apt.graph.model.storage.classpath.FileStore;
import com.salesforce.aptspring.processor.VerifiedSpringConfiguration;

public class AptAnnotationParserTest {
  
  @Test
  public void testAnnoProcMetaAnnotations() throws IOException {
    JavaFileObject annotation = JavaFileObjects.forSourceLines(
            "test.BeanWrapper",
            "package test;",
            "",
            "import org.springframework.beans.factory.annotation.Qualifier;",
            "import org.springframework.context.annotation.Bean;",
            "import org.springframework.core.annotation.AliasFor;",
            "import java.lang.annotation.Documented;",
            "import java.lang.annotation.ElementType;",
            "import java.lang.annotation.Retention;",
            "import java.lang.annotation.RetentionPolicy;",
            "import java.lang.annotation.Target;",
            "",
            "@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})",
            "@Retention(RetentionPolicy.RUNTIME)",
            "@Documented",
            "@Bean",
            "public @interface BeanWrapper {",
            " ",
            "  @AliasFor(annotation=Bean.class, value=\"name\")",
            "  String[] otherName() default \"\";",
            "",
            "}"
    );
    
    JavaFileObject definitionClass = JavaFileObjects.forSourceLines(
            "test.TestClass",
            "package test;",
            "",
            "import org.springframework.beans.factory.annotation.Qualifier;",
            "import org.springframework.context.annotation.Bean;",
            "import org.springframework.context.annotation.Configuration;",
            "",
            "  @com.salesforce.aptspring.Verified",
            "  public class TestClass {",
            "",
            "    @BeanWrapper(otherName = \"value1\")",
            "    public String value1(@Qualifier(\"value2\") String x) { return \"\";}",
            "",
            "    @BeanWrapper(otherName = \"value2\")",
            "    public String value2() { return \"\";}",
            "",
            "}");
    
    assertAbout(javaSources())
            .that(Arrays.asList(annotation, definitionClass))
            .processedWith(new VerifiedSpringConfiguration())
            .compilesWithoutError()
            .and()
            .generatesFileNamed(StandardLocation.SOURCE_OUTPUT, "test", "TestClass_" + FileStore.STANDARD.getPath() + ".java")
            .and()
            .generatesFileNamed(StandardLocation.CLASS_OUTPUT, "test", "TestClass_" + FileStore.STANDARD.getPath() + ".class");
  }
  
  @Test
  public void testAnnoProcMetaAnnotation3() throws IOException {
    JavaFileObject definitionClass = JavaFileObjects.forSourceLines(
            "test.TestClass",
            "package test;",
            "",
            "import org.springframework.beans.factory.annotation.Qualifier;",
            "import org.springframework.context.annotation.Bean;",
            "import org.springframework.context.annotation.Configuration;",
            "",
            "  @com.salesforce.aptspring.Verified",
            "  public class TestClass {",
            "",
            "    @Override",
            "    public String value1(@Qualifier(\"value2\") String x) { return \"\";}",
            "",
            "    @Bean",
            "    public String value2() { return \"\";}",
            "",
            "}");
    
    assertAbout(javaSources())
            .that(Arrays.asList(definitionClass))
            .processedWith(new VerifiedSpringConfiguration())
            .failsToCompile()
            .withErrorContaining("All methods on @Configuration must have @Bean annotation")
            .in(definitionClass)
            .onLine(11)
            .and()
            .withErrorContaining("All @Bean annotations must define at least one name for a bean.")
            .in(definitionClass)
            .onLine(14);
            
  }
  
  
  
}
