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
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.tools.Diagnostic.Kind;

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
  
  private static final String COMPONENT_TYPE = "org.springframework.stereotype.Component";
    
  private static final String AUTOWIRED_TYPE = "org.springframework.beans.factory.annotation.Autowired";
  
  private static final String DEFAULT_ANNOTATION_VALUE = "value";

  /**
   * Will return true if a class level contains exactly a constant final static private literal field.
   */
  private Predicate<VariableElement> staticPrivateFinalLiteralField = ve -> ve.getModifiers()
      .containsAll(Arrays.asList(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL))
      && ve.getModifiers().size() == 3
      && ve.getConstantValue() != null;

  /**
   * Will return true if a class level contains exactly a final private field without a constant value.
   */
  private Predicate<VariableElement> privateFinalField = ve -> ve.getModifiers()
      .containsAll(Arrays.asList(Modifier.PRIVATE, Modifier.FINAL))
      && ve.getModifiers().size() == 2
      && ve.getConstantValue() == null;  
 
  /**
   * Read a TypeElement to get application structure.
   * 
   * @param te definition type element.
   * @param messager presents error messages for the compiler to pass to the user.
   * @return the {@link DefinitionModel} parsed from a properly annotated {@link TypeElement}
   * @deprecated please see {@link SpringAnnotationParser#extractDefinition(TypeElement, Messager)}
   */
  public static DefinitionModel parseDefinition(TypeElement te, Messager messager) {  
    return new SpringAnnotationParser().extractDefinition(te, messager);
  }
  
  /**
   * Read a TypeElement to get application structure.
   * 
   * @param te definition type element.
   * @param messager presents error messages for the compiler to pass to the user.
   * @return the {@link DefinitionModel} parsed from a properly annotated {@link TypeElement}
   */
  public DefinitionModel extractDefinition(TypeElement te, Messager messager) {  
    Verified verified = te.getAnnotation(Verified.class);
    DefinitionModel model = new DefinitionModel(te, verified == null ? false : verified.root());

    errorIfInvalidClass(te, messager);
    
    model.addDependencyNames(getImportsTypes(te));
    String[] configurationBeanNames  = AnnotationValueExtractor
        .getAnnotationValue(te, CONFIGURATION_TYPE, DEFAULT_ANNOTATION_VALUE);
    String[] componentBeanNames  = AnnotationValueExtractor
        .getAnnotationValue(te, COMPONENT_TYPE, DEFAULT_ANNOTATION_VALUE);
    if (configurationBeanNames != null) {
      for (Element enclosed : te.getEnclosedElements()) {
        handleEnclosedElements(messager, model, enclosed);
      }
    } else {
      if (componentBeanNames != null) {
        addModelsFromComponent(te, model, componentBeanNames, messager);
      } else {
        messager.printMessage(Kind.ERROR, "@Verified annotation must only be used on @Configuration or @Component classes", te);
      }
    }
    
    for (String expectedBean : verified.expectedBeans()) {
      model.addDefinition(new ExpectedModel(expectedBean, te));
    }
    return model;
  }
  
  private List<Modifier> getIllegalModifiers(Set<Modifier> existing, List<Modifier> illegal) {
    List<Modifier> modifiers = new ArrayList<>(existing);
    modifiers.removeIf(modifier -> !illegal.contains(modifier));
    modifiers.sort((m1, m2) ->  m1.name().compareTo(m2.name())); //in case someone reorders
    return modifiers;
  }
  
  private boolean parseBeanMethod(ExecutableElement beanMethod, String[] beanNames, Messager messager) {
    boolean valid = true;
    if (beanNames.length == 0) {
      valid = false;
      messager.printMessage(Kind.ERROR, "All @Bean annotations must define at least one name for a bean.", beanMethod);
    }
    if (beanMethod.getReturnType().getKind() != TypeKind.DECLARED) {
      valid = false;
      messager.printMessage(Kind.ERROR, "@Bean methods must return an Object", beanMethod);
    }
    if (!beanMethod.getModifiers().contains(Modifier.PUBLIC)) {
      valid = false;
      messager.printMessage(Kind.ERROR, "@Bean methods must be marked public", beanMethod);
    }
    List<Modifier> illegalModifiers = getIllegalModifiers(beanMethod.getModifiers(), DISALLOWED_ON_METHOD);
    if (illegalModifiers.size() != 0) {
      valid = false;
      messager.printMessage(Kind.ERROR, "Illegal modifiers found on spring @Bean method: "
           + illegalModifiers.stream().map(m -> m.name()).collect(Collectors.joining(", ")),
          beanMethod);
    }
    return valid;
  }

  private void handleEnclosedElements(Messager messager, DefinitionModel model, Element enclosed) {
    switch (enclosed.getKind()) {
      case METHOD: 
        ExecutableElement execelement = (ExecutableElement) enclosed;
        String[] beanNames = AnnotationValueExtractor
            .getAnnotationValue(execelement, "org.springframework.context.annotation.Bean", "name");
        
        if (beanNames != null) {
          List<InstanceDependencyModel> dependencies = execElementDependency(messager, model, execelement);          
          if (parseBeanMethod(execelement, beanNames, messager)) {
            List<String> names = new ArrayList<>(Arrays.asList(beanNames));
            String defaultName = names.get(0);
            names.remove(defaultName);
            model.addDefinition(new InstanceModel(defaultName, model.getIdentity(), execelement,
                execelement.getReturnType().toString(), dependencies, names));
          }
        } else {
          messager.printMessage(Kind.ERROR, "All methods on @Configuration must have @Bean annotation", execelement);
        }
        break;
      case FIELD:
        if (!staticPrivateFinalLiteralField.test((VariableElement) enclosed)) {
          messager.printMessage(Kind.ERROR, "Only private static final constants are permitted in @Verified @Configuration classes",
              enclosed);
        }
        break;
      case ENUM_CONSTANT: 
        if (!staticPrivateFinalLiteralField.test((VariableElement) enclosed)) {
          messager.printMessage(Kind.ERROR, "Only private static final constants are permitted in @Verified @Configuration classes",
              enclosed);
        }
        break;
      case CONSTRUCTOR:
        ExecutableElement constelement = (ExecutableElement) enclosed;
        if (!constelement.getModifiers().contains(Modifier.PUBLIC)) {
          messager.printMessage(Kind.ERROR, "@Configuration should not have any non-public constructors.", enclosed);
        }
        if (constelement.getParameters().size() > 0) {
          messager.printMessage(Kind.ERROR, "@Configuration should not have any non-defualt constructors.", enclosed);
        }
        break;
      default:
        messager.printMessage(Kind.ERROR, "Only @Bean methods, private static final literals, and default constructors "
            + "are allowed on @Configuration classes", enclosed);
        break;
    }
  }

  /**
   * This method is called on {@link ExecutableElement}.
   * Bean methods on an @Configuration bean, or Constructors on @Component classes.
   * This method parses the @Qualifier, or @Value annotations if a @Verified=root, and reads the types of each parameter.
   * That data is used to build a list of {@link InstanceDependencyModel}'s which are part of an {@link InstanceModel}.
   * All parameters must have an @Qualifier or @Value, and the annotations can not be mixed, errors will result otherwise.
   * 
   * @param messager APT messager that will receive error messages.
   * @param model the DefinitionModel being parse, which may be a @Configuration or @Component annotated entity.
   * @param execelement the bean method if an @Configuration, or the constructor if an @Component.
   * @return the dependencies of the to be constructed {@link InstanceModel}
   */
  private List<InstanceDependencyModel> execElementDependency(Messager messager, DefinitionModel model,
      ExecutableElement execelement) {
    List<InstanceDependencyModel> dependencies = new ArrayList<>();
    boolean hasValues = false;
    boolean hasQualifiers = false;
    for (VariableElement varelement : execelement.getParameters()) {
      
      String[] qualifierNames = AnnotationValueExtractor
          .getAnnotationValue(varelement, QUALIFIER_TYPE, DEFAULT_ANNOTATION_VALUE);
      String[] valueNames = AnnotationValueExtractor
          .getAnnotationValue(varelement, VALUE_TYPE, DEFAULT_ANNOTATION_VALUE);
      
      if (qualifierNames == null && valueNames == null) {
        messager.printMessage(Kind.ERROR, "All parameters must have an @Qualifier or a @Value annotation", varelement);
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
      messager.printMessage(Kind.ERROR, "No method may define both @Qualifier or a @Value annotations,"
          + " keep property values in there own beans", execelement);
    }
    if (hasValues &&  !model.isRootNode()) {
      messager.printMessage(Kind.ERROR, "Only @Verified(root=true) nodes may use @Value annotations to create beans,"
          + " decouples spring graph from environment", execelement);
    }
    return dependencies;
  }

  /**
   * Builds an instance model by finding the autowired constructor of an @Component model as well as 
   * 
   * @param te the TypeElement corresponding to the @Component class.
   * @param dm the definitionModel built from the @Component.
   * @param names the list if names in an @Component annotation.  Users must explicitly define one.
   * @param messager errors are added to this APT messager.
   */
  private void addModelsFromComponent(TypeElement te, DefinitionModel dm, String[] names, Messager messager) {
    List<InstanceDependencyModel> dependencies = new ArrayList<>();
    ExecutableElement chosenConstructor = findAutowiredConstructor(extractConstructorsFromComponent(te));
    if (chosenConstructor == null) {
      messager.printMessage(Kind.ERROR, "No single default constructor or single @Autowired constructor", te);
    } else {
      dependencies = execElementDependency(messager, dm, chosenConstructor);
      //dm.getExpectedDefinitions()
    }
    te.getEnclosedElements().stream()
        .filter(el -> el instanceof VariableElement)
        .map(el -> (VariableElement) el)
        .filter(ve -> !staticPrivateFinalLiteralField.test(ve) && !privateFinalField.test(ve))
        .forEach(ve -> messager
            .printMessage(Kind.ERROR, "@Component classes my only have static final constant fields or final private fields", ve));
    
    InstanceModel model = new InstanceModel(names[0],
        dm.getIdentity(), 
        chosenConstructor, 
        te.getQualifiedName().toString(),
        dependencies, 
        new ArrayList<>());
    
    dm.addDefinition(model);
    for (InstanceDependencyModel dep : dependencies) {
      ExpectedModel expectedModel = new ExpectedModel(dep.getIdentity());
      expectedModel.addDefinitionReferenceToType(model.getIdentity(), dep.getType());
      dm.addDefinition(expectedModel);
    }
  }

  /**
   * Analyzes a list of constructors from an @Component, looking for a single constructor, or if multiple
   * constructors exist, a single constructor marked with @Autowire.
   *
   * @param constructors a list of constructors from an @Component.
   * @return the executable element, or null.
   */
  private ExecutableElement findAutowiredConstructor(List<ExecutableElement> constructors) {
    ExecutableElement chosenConstructor = null;
    if (constructors.size() == 1) {
      chosenConstructor = constructors.get(0);
    } else {
      chosenConstructor = constructors.stream()
        .filter(ex -> AnnotationValueExtractor.getAnnotationValue(ex, AUTOWIRED_TYPE, "") != null)
        .limit(2) //stop at two. efficiency.
        .reduce((a, b) -> null) //if more than one return null.
        .orElse(null);
    }
    return chosenConstructor;
  }

  /**
   * Given an @Component's {@link TypeElement} find's all constructors of that type.
   * 
   * @param te a representation of an @Component class.
   * @return a list of executable elements representing all found constructors.
   */
  private List<ExecutableElement> extractConstructorsFromComponent(TypeElement te) {
    return te.getEnclosedElements().stream()
      .filter(enclosed -> enclosed instanceof ExecutableElement)
      .filter(enclosed -> "<init>".equals(enclosed.getSimpleName().toString()))
      .map(enclosed -> (ExecutableElement) enclosed)
      .collect(Collectors.toList());
  }

  private void errorIfInvalidClass(TypeElement te, Messager messager) {
    if (te.getEnclosingElement().getKind() != ElementKind.PACKAGE) {
      messager.printMessage(Kind.ERROR, "The class must be a top level class, not an internal class", te);
    }
    if (AnnotationValueExtractor.getAnnotationValue(te, COMPONENTSCAN_TYPE, "basePackages") != null
        || AnnotationValueExtractor.getAnnotationValue(te, COMPONENTSCANS_TYPE, "basePackages") != null) {
      messager.printMessage(Kind.ERROR, "You may not use @ComponentScan(s) on @Verified classes", te);
    }
  }
  
  private List<String> getImportsTypes(TypeElement element) {
    String[] values = AnnotationValueExtractor
        .getAnnotationValue(element, "org.springframework.context.annotation.Import", DEFAULT_ANNOTATION_VALUE);
    if (values == null) {
      return new ArrayList<>();
    } else {
      return Arrays.asList(values);
    }
  }

}