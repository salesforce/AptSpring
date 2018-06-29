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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

import com.salesforce.apt.graph.model.errors.ErrorMessages;
import com.salesforce.apt.graph.parser.apt.AptElementVisitor;
import com.salesforce.apt.graph.parser.apt.AptParsingContext;
import com.salesforce.aptspring.Verified;

@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class VerifiedSpringConfiguration extends AbstractProcessor {

  private Messager messager;
  
  private AptParsingContext definitionAggregator;

  @Override
  public synchronized void init(ProcessingEnvironment env) {
    super.init(env);
    Types typeUtils = env.getTypeUtils();
    Elements elementUtils = env.getElementUtils();
    Filer filer = env.getFiler();
    messager = env.getMessager();
    ErrorMessages errorMessages = ErrorMessages.builder()
        .cycleInDefinitionSources("Cycle in @Imports {0}")
        .cycleInObjectDefinitions("Cycle in spring bean dependencies {0}")
        .duplicateObjectDefinitions("Duplicate in spring beans {0}")
        .nonLiteralStaticMemberVariables("Non literal static member variables can break class instantiation and leak state")
        .knownDamagingClass("Known damaging class import (contains non-literal static member variables)")
        .missingBeanDefinitions("Missing bean definitions for spring beans {0},"
           + " create definitions or list them in @Verified'''s expected field")
        .missingRelevantAnnotations("Missing @Verified on classes {0}")
        .unmatchedTypes("Unmatched types {0}")
        .duplicatedMatchingDependencies("Duplicated matching dependencies {0}")
        .duplicatedMatchingDefinitions("Duplicated matching definitions {0}")
        .noMatchingDefinition("No matching definition {0}")
        .unusedExpected("Expected bean name is unnecessary {0}")
        .couldNotStore("Could not store incremental build file for {0}")
        .couldNotRead("Could not read incremental build file for {0}")
        .dependencyShaMismatch("Sha256 mismatch of dependency model of prior analyzed @Verified class model {0}")
        .rootNodeImported("@Verfied(root=true) may not be @Imported by other @Verified classes: {0}")
        .build();
    definitionAggregator = new AptParsingContext(errorMessages, filer, elementUtils, typeUtils);
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {
    try  {
      if (env.processingOver()) {
     	 messager.printMessage(Diagnostic.Kind.NOTE, "AptSpring processing over on: " 
   	          + env.getElementsAnnotatedWith(Verified.class).stream().map(a -> a.toString()).collect(Collectors.joining(", ")));
        definitionAggregator.outputErrors(messager);
      } else {
        AptElementVisitor visitor = new AptElementVisitor(te -> new SpringAnnotationParser().extractDefinition(te, messager));
        messager.printMessage(Diagnostic.Kind.NOTE, "AptSpring processing on: " 
     	          + env.getElementsAnnotatedWith(Verified.class).stream().map(a -> a.toString()).collect(Collectors.joining(", ")));
        for (Element annotatedElement : env.getElementsAnnotatedWith(Verified.class)) {
          visitor.visit(annotatedElement, definitionAggregator);        
        }
      }
      return true;
    } catch (Exception exception) {
      // Catch and print for debugging reasons. This code path is unexpected.
      StringWriter writer = new StringWriter();
      exception.printStackTrace(new PrintWriter(writer));
      messager.printMessage(Diagnostic.Kind.ERROR, writer.toString());
      return true;
    }
  }

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    Set<String> output = new HashSet<>();
    output.add(Verified.class.getName());
    return output;
  }
}