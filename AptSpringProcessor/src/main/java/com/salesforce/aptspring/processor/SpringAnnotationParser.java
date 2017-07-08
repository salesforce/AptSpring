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

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScans;
import org.springframework.context.annotation.Configuration;

import com.salesforce.apt.graph.model.DefinitionModel;
import com.salesforce.apt.graph.model.ExpectedModel;
import com.salesforce.apt.graph.model.InstanceDependencyModel;
import com.salesforce.apt.graph.model.InstanceModel;
import com.salesforce.aptspring.Verified;

public class SpringAnnotationParser {
  
  private static class AliasData {
    private String targetAnnotation = null;
    private String targetField = null;
  }
  
  /**
   * On an executable element (that is a value holder on annotation) extract any
   * direct uses of @AlaisFor
   * 
   * @param annotationParameter the annotation's parameter to inspect for uses of @AliasFor
   * @return an AliasData if the the annotation is found, null otherwise.
   */
  public static AliasData getAlias(ExecutableElement annotationParameter) {
    AliasData output = null;
    for (AnnotationMirror am : annotationParameter.getAnnotationMirrors()) {
      if (ALIAS_TYPE.equals(am.getAnnotationType().asElement().toString())) {
        output = new AliasData();
        for (Entry<? extends ExecutableElement, ? extends AnnotationValue> ev : am.getElementValues().entrySet()) {
          String fieldName = ev.getKey().getSimpleName().toString();
          if (ALIAS_TARGET_TYPE.equals(fieldName)) {
            if (ev.getValue() != null && ev.getValue().getValue() != null) {
              output.targetAnnotation = ev.getValue().getValue().toString();
            } else {
              return null;
            }
          }
          if (ALIAS_TARGET_FIELD.equals(fieldName)) {
            if (ev.getValue() != null && ev.getValue().getValue() != null) {
              output.targetField = ev.getValue().getValue().toString();
            }
          }
          if ("value".equals(fieldName)) {
            if (ev.getValue() != null && ev.getValue().getValue() != null) {
              output.targetField = ev.getValue().getValue().toString();
            }
          }
        }
      }
    }
    return output;
  }
  
  /**
   *  Checks to see if the aliasData matches the targetType and targetField.   The aliasData may have a null
   *  targetType and if so, the currentAnnotation is used to determine if the targetType Matches.
   *  This indicates that the AliasFor annotation is on an element in the targetType annotation itself.
   */
  private static boolean aliasMatch(AliasData aliasData, String targetType, String targetField, String currentAnnotation) {
    if (aliasData == null) {
      return false;
    }
    //types match
    if (targetType.equals(aliasData.targetAnnotation)
        || (aliasData.targetAnnotation == null && targetType.equals(currentAnnotation))) {
      //fields match
      if (targetField.equals(aliasData.targetField)) {
        return true;
      }
    }
    return false;
  }
  
  private static final String ALIAS_TYPE = "org.springframework.core.annotation.AliasFor";
  
  private static final String ALIAS_TARGET_TYPE = "annotation";

  private static final String ALIAS_TARGET_FIELD = "attribute";

  /**
   * Utility method to extract the value of annotation on a class.
   * Hooks to honor spring's @Verifed annotation.
   * 
   * @param e the element to inspect
   * @param annotationTypeName the fully qualified name of the annotation class.
   * @param methodName the name of the annotation value
   * @return an array of Strings representing the value of annotation parameter or it's alias.
   */
  public static String[] getAnnotationValue(Element e, String annotationTypeName, String methodName) {
    if (e instanceof TypeElement) {
      //TODO: do recursive call in to 
      ((TypeElement) e).getSuperclass();
      ((TypeElement) e).getInterfaces();
    }
    for (AnnotationMirror a : e.getAnnotationMirrors()) {
      String[] returned = getAnnotationValue(a, annotationTypeName, methodName);
      if (returned != null) {
        return returned;
      }
    }
    return null;
  }
  
  /**
   * Any empty array will be returned as long as the annotation is found (regardless of whether the value is set or not).
   * A null value is returned if the (meta) annotation is not found. Currently only supports one level of indirection through
   * spring's AliasFor.
   *
   * @param am the annotation to parse for a value.
   * @param annotationTypeName the type of the annotation we are interested in, necessary for meta-annotation processing.
   * @param methodName the name of the parameter designating the value 
   * @return if the annotation or meta annotation is found, the AnnotationValues are converted to strings by 
   *    {@link AnnotationValueExtractor} and returned in an array.  
   */
  public static String[] getAnnotationValue(AnnotationMirror am, String annotationTypeName, String methodName) {
    String currentType = am.getAnnotationType().toString();
    for (Entry<? extends ExecutableElement, ? extends AnnotationValue> ev : am.getElementValues().entrySet()) {
      boolean aliasMatch = aliasMatch(getAlias(ev.getKey()), annotationTypeName, methodName, currentType);
      boolean foundField = ev.getKey().getSimpleName().toString().equals(methodName);
      if (aliasMatch || (foundField && currentType.equals(annotationTypeName))) {
        AnnotationValueExtractor ex = new AnnotationValueExtractor();
        List<String> values = new ArrayList<>();
        ex.visit(ev.getValue(), values);
        return values.toArray(new String[values.size()]); 
      }
    }
    if (currentType.equals(annotationTypeName)) {
      //no field matched
      return new String[]{};
    }
    
    for (AnnotationMirror a : am.getAnnotationType().getAnnotationMirrors()) {
      //cachable here...
      if (!a.getAnnotationType().asElement().toString().startsWith("java.lang.annotation")) {
        String[] output = getAnnotationValue(a, annotationTypeName, methodName);
        if (output != null) {
          return output;
        }
      }
    }
    return null;
  }
  
  /**
   * Read a TypeElement to get application structure.
   * 
   * @param te
   *          definition type element.
   */
  public static DefinitionModel parseDefinition(TypeElement te, Messager messager) {  
    Verified verified = te.getAnnotation(Verified.class);
    DefinitionModel model = new DefinitionModel(te, verified == null ? false : verified.root());

    errorIfInvalidClass(te, messager);
    
    model.addDependencyNames(getImportsTypes(te));
    Configuration configuration = te.getAnnotation(Configuration.class);
    if (configuration != null) {
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
        String[] beanNames = getAnnotationValue(execelement, "org.springframework.context.annotation.Bean", "name");
        
        //Bean beanElement = execelement.getAnnotation(Bean.class);
        if (beanNames != null) {
          List<InstanceDependencyModel> dependencies = new ArrayList<>();
          boolean hasValues = false;
          boolean hasQualifiers = false;
          for (VariableElement varelement : execelement.getParameters()) {
            Qualifier qualifier = varelement.getAnnotation(Qualifier.class);
            Value value = varelement.getAnnotation(Value.class);
            if (qualifier == null && value == null) {
              messager.printMessage(javax.tools.Diagnostic.Kind.ERROR,
                  "All parameters must have an @Qualifier or a @Value annotation", varelement);
            } 
            if (qualifier != null) {
              dependencies.add(new InstanceDependencyModel(qualifier.value(), varelement.asType().toString()));
              hasQualifiers = true;
            }
            if (value != null) {
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
    }
  }

  private static void errorIfInvalidClass(TypeElement te, Messager messager) {
    if (te.getEnclosingElement().getKind() != ElementKind.PACKAGE) {
      messager.printMessage(javax.tools.Diagnostic.Kind.ERROR,
          "The class must be a top level class, not an internal class", te);
    }
    if (te.getAnnotation(ComponentScan.class) != null
        || te.getAnnotation(ComponentScans.class) != null) {
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
          if ("value".equals(entry.getKey().getSimpleName().toString())) {
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