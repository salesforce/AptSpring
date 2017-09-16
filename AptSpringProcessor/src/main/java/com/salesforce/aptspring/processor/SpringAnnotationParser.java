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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.processing.Messager;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import com.salesforce.apt.graph.model.DefinitionModel;
import com.salesforce.apt.graph.model.ExpectedModel;
import com.salesforce.apt.graph.model.InstanceDependencyModel;
import com.salesforce.apt.graph.model.InstanceModel;
import com.salesforce.aptspring.Verified;

public class SpringAnnotationParser {
  
  private static final List<Modifier> DISALLOWED_ON_METHOD = Collections.unmodifiableList(Arrays.asList(
      Modifier.ABSTRACT,
      Modifier.DEFAULT,
      Modifier.FINAL,
      Modifier.NATIVE,
      Modifier.STATIC,
      Modifier.VOLATILE
      //Modifier.PRIVATE, //checks to see if the method is public, these checks would be redundant.
      //Modifier.PROTECTED
      ));
  
  private static final String QUALIFIER_TYPE = "org.springframework.beans.factory.annotation.Qualifier";
  
  private static final String VALUE_TYPE = "org.springframework.beans.factory.annotation.Value";
  
  private static final String COMPONENTSCAN_TYPE = "org.springframework.context.annotation.ComponentScan";
  
  private static final String COMPONENTSCANS_TYPE = "org.springframework.context.annotation.ComponentScans";
  
  private static final String CONFIGURATION_TYPE = "org.springframework.context.annotation.Configuration";
    
  private static final String DEFAULT_ANNOTATION_VALUE = "value";

  /**
   * Read a TypeElement to get application structure.
   * 
   * @param te definition type element.
   * @param messager presents error messages for the compiler to pass to the user.
   * @return the {@link DefinitionModel} parsed from a properly annotated {@link TypeElement}
   */
  public static DefinitionModel parseDefinition(TypeElement te, Messager messager) {  
    Verified verified = te.getAnnotation(Verified.class);
    DefinitionModel model = new DefinitionModel(te, verified == null ? false : verified.root());

    errorIfInvalidClass(te, messager);
    
    model.addDependencyNames(getImportsTypes(te));
    String[] configurationBeanNames  = AnnotationValueExtractor
        .getAnnotationValue(te, CONFIGURATION_TYPE, DEFAULT_ANNOTATION_VALUE);
    if (configurationBeanNames != null) {
      for (Element enclosed : te.getEnclosedElements()) {
        handleEnclosedElements(messager, model, enclosed);
      }
    } else {
      messager.printMessage(javax.tools.Diagnostic.Kind.ERROR,
          "@Verified annotation must only be used on @Configuration classes", te);
    }
    
    for (String expectedBean : verified.expectedBeans()) {
      model.addDefinition(new ExpectedModel(expectedBean, te));
    }
    return model;
  }
  
  private static List<Modifier> getIllegalModifiers(Set<Modifier> existing, List<Modifier> illegal) {
    List<Modifier> modifiers = new ArrayList<>(existing);
    modifiers.removeIf(modifier -> !illegal.contains(modifier));
    modifiers.sort((m1, m2) ->  m1.name().compareTo(m2.name())); //in case someone reorders
    return modifiers;
  }
  
  private static boolean checkExecElement(ExecutableElement execelement, String[] beanNames, Messager messager) {
    boolean valid = true;
    if (beanNames.length == 0) {
      valid = false;
      messager.printMessage(javax.tools.Diagnostic.Kind.ERROR,
          "All @Bean annotations must define at least one name for a bean.", execelement);
    }
    if (execelement.getReturnType().getKind() != TypeKind.DECLARED) {
      valid = false;
      messager.printMessage(javax.tools.Diagnostic.Kind.ERROR,
          "@Bean methods must return an Object", execelement);
    }
    if (!execelement.getModifiers().contains(Modifier.PUBLIC)) {
      valid = false;
      messager.printMessage(javax.tools.Diagnostic.Kind.ERROR,
          "@Bean methods must be marked public", execelement);
    }
    
    List<Modifier> illegalModifiers = getIllegalModifiers(execelement.getModifiers(), DISALLOWED_ON_METHOD);
    if (illegalModifiers.size() != 0) {
      valid = false;
      messager.printMessage(javax.tools.Diagnostic.Kind.ERROR,
          "Illegal modifiers found on spring @Bean method: "
           + illegalModifiers.stream().map(m -> m.name()).collect(Collectors.joining(", ")),
          execelement);
    }
    return valid;
  }

  private static void handleEnclosedElements(Messager messager, DefinitionModel model, Element enclosed) {
    switch (enclosed.getKind()) {
      case METHOD: 
        ExecutableElement execelement = (ExecutableElement) enclosed;
        String[] beanNames = AnnotationValueExtractor
            .getAnnotationValue(execelement, "org.springframework.context.annotation.Bean", "name");
        
        if (beanNames != null) {
          List<InstanceDependencyModel> dependencies = new ArrayList<>();
          boolean hasValues = false;
          boolean hasQualifiers = false;
          for (VariableElement varelement : execelement.getParameters()) {
            
            String[] qualifierNames = AnnotationValueExtractor
                .getAnnotationValue(varelement, QUALIFIER_TYPE, DEFAULT_ANNOTATION_VALUE);
            String[] valueNames = AnnotationValueExtractor
                .getAnnotationValue(varelement, VALUE_TYPE, DEFAULT_ANNOTATION_VALUE);
            
            if (qualifierNames == null && valueNames == null) {
              messager.printMessage(javax.tools.Diagnostic.Kind.ERROR,
                  "All parameters must have an @Qualifier or a @Value annotation", varelement);
            } 
            if (qualifierNames != null) {
              dependencies.add(new InstanceDependencyModel(qualifierNames[0], varelement.asType().toString()));
              hasQualifiers = true;
            }
            if (valueNames != null) {
              //ignore values as they will be used to build beans and pass the data on, and
              //are not beans themselves... and cannot be intermingled with @Qualifiers.
              hasValues = true;
            }
          }
          if (hasValues && hasQualifiers) {
            messager.printMessage(javax.tools.Diagnostic.Kind.ERROR,
                "No method may define both @Qualifier or a @Value annotations,"
                + " keep property values in there own beans", execelement);
          }
          if (hasValues &&  !model.isRootNode()) {
            messager.printMessage(javax.tools.Diagnostic.Kind.ERROR,
                "Only @Verified(root=true) nodes may use @Value annotations to create beans,"
                + " decouples spring graph from environment", execelement);
          }          
          if (checkExecElement(execelement, beanNames, messager)) {
            List<String> names = new ArrayList<>(Arrays.asList(beanNames));
            String defaultName = names.get(0);
            names.remove(defaultName);
            model.addDefinition(new InstanceModel(defaultName, model.getIdentity(), execelement,
                execelement.getReturnType().toString(), dependencies, names));
          }
        } else {
          messager.printMessage(javax.tools.Diagnostic.Kind.ERROR,
              "All methods on @Configuration must have @Bean annotation", execelement);
        }
        break;
      case FIELD:
        errorNonLiteralStaticFields((VariableElement) enclosed, messager);
        break;
      case ENUM_CONSTANT: 
        errorNonLiteralStaticFields((VariableElement) enclosed, messager);
        break;
      case CONSTRUCTOR:
        ExecutableElement constelement = (ExecutableElement) enclosed;
        if (!constelement.getModifiers().contains(Modifier.PUBLIC)) {
          messager.printMessage(javax.tools.Diagnostic.Kind.ERROR,
              "@Configuration should not have any non-public constructors.",
              enclosed);
        }
        if (constelement.getParameters().size() > 0) {
          messager.printMessage(javax.tools.Diagnostic.Kind.ERROR,
              "@Configuration should not have any non-defualt constructors.",
              enclosed);
        }
        break;
      default:
        messager.printMessage(javax.tools.Diagnostic.Kind.ERROR,
            "Only @Bean methods, private static final literals, and default constructors are allowed on @Configuration classes",
            enclosed);
        break;
    }
  }

  private static void errorIfInvalidClass(TypeElement te, Messager messager) {
    if (te.getEnclosingElement().getKind() != ElementKind.PACKAGE) {
      messager.printMessage(javax.tools.Diagnostic.Kind.ERROR,
          "The class must be a top level class, not an internal class", te);
    }
    if (AnnotationValueExtractor.getAnnotationValue(te, COMPONENTSCAN_TYPE, "basePackages") != null
        || AnnotationValueExtractor.getAnnotationValue(te, COMPONENTSCANS_TYPE, "basePackages") != null) {
      messager.printMessage(javax.tools.Diagnostic.Kind.ERROR,
          "You may not use @ComponentScan(s) on @Verified classes", te);
    }
  }
  
  private static void errorNonLiteralStaticFields(VariableElement element, Messager messager) {
    if (element.getModifiers().isEmpty() 
        || !(element.getModifiers().contains(Modifier.PRIVATE))
        || !(element.getModifiers().contains(Modifier.STATIC))
        || !(element.getModifiers().contains(Modifier.FINAL))
        || element.getConstantValue() == null) {
      messager.printMessage(javax.tools.Diagnostic.Kind.ERROR,
          "Only private static final constants are permitted in @Verified @Configuration classes", element);
    }
  }

  
  private static List<String> getImportsTypes(TypeElement element) {
    final List<String> importedDefinitions = new ArrayList<>();
    for (AnnotationMirror am : element.getAnnotationMirrors()) {
      if ("org.springframework.context.annotation.Import".equals(am.getAnnotationType().toString())) {
        for (Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : am.getElementValues().entrySet()) {
          if (DEFAULT_ANNOTATION_VALUE.equals(entry.getKey().getSimpleName().toString())) {
            @SuppressWarnings("unchecked")
            List<? extends AnnotationValue> value = (List<? extends AnnotationValue>) entry.getValue().getValue();
            for (AnnotationValue av : value) {
              importedDefinitions.add(((TypeMirror) av.getValue()).toString());
            }
          }
        }
      }
    }
    return importedDefinitions;
  }

}