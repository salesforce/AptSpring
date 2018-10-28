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

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.SimpleAnnotationValueVisitor8;

public class AnnotationValueExtractor {

  private static final String ALIAS_TYPE = "org.springframework.core.annotation.AliasFor";
  
  private static final String ALIAS_TARGET_TYPE = "annotation";

  private static final String ALIAS_TARGET_FIELD = "attribute";
  
  private static final String DEFAULT_ANNOTATION_VALUE = "value";

  
  private static class AliasData {
    private String targetAnnotation = null;
    private String targetField = null;
  }
  
  /**
   * Utility method to extract the value of annotation on a class.
   * Hooks to honor spring's AliasFor annotation, see {@link AnnotationValueExtractor#ALIAS_TYPE}.
   * 
   * @param e the element to inspect
   * @param annotationTypeName the fully qualified name of the annotation class.
   * @param methodName the name of the annotation value
   * @return an array of Strings representing the value of annotation parameter or it's alias.
   *     null if the annotation is not present (or is in a wrapper annotation as an array of values),
   *     an empty array is returned if the annotation is present, but the method does not exist.
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
  private static String[] getAnnotationValue(AnnotationMirror am, String annotationTypeName, String methodName) {
    String currentType = am.getAnnotationType().toString();
    for (Entry<? extends ExecutableElement, ? extends AnnotationValue> ev : am.getElementValues().entrySet()) {
      boolean aliasMatch = aliasMatch(getAlias(ev.getKey()), annotationTypeName, methodName, currentType);
      boolean foundField = ev.getKey().getSimpleName().toString().equals(methodName);
      if (aliasMatch || (foundField && currentType.equals(annotationTypeName))) {
        AnnotationValueExtractorVisitor ex = new AnnotationValueExtractorVisitor();
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
   * On an executable element (that is a value holder on annotation) extract any direct uses of @AlaisFor.
   * 
   * @param annotationParameter the annotation's parameter to inspect for uses of @AliasFor
   * @return an AliasData if the the annotation is found, null otherwise.
   */
  private static AliasData getAlias(ExecutableElement annotationParameter) {
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
          if (ALIAS_TARGET_FIELD.equals(fieldName) 
              && ev.getValue() != null && ev.getValue().getValue() != null) {
            output.targetField = ev.getValue().getValue().toString();
          }
          if (DEFAULT_ANNOTATION_VALUE.equals(fieldName)
              && (ev.getValue() != null && ev.getValue().getValue() != null)) {
            output.targetField = ev.getValue().getValue().toString();
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
    return (//types match
        (targetType.equals(aliasData.targetAnnotation)
        || (aliasData.targetAnnotation == null && targetType.equals(currentAnnotation)))
        && //fields match
        targetField.equals(aliasData.targetField));
  }
  
  private static class AnnotationValueExtractorVisitor extends SimpleAnnotationValueVisitor8<Void, List<String>> {

    @Override
    protected Void defaultAction(Object o, List<String> values) {
      values.add(o.toString());
      return null;
    }

    public Void visitEnumConstant(VariableElement c, List<String> values) {
      values.add(c.getSimpleName().toString());
      return null;
    }

    public Void visitAnnotation(AnnotationMirror a, List<String> values) {
      // should probably do something here, but what? return annotation types?
      return defaultAction(a, values);
    }

    public Void visitArray(List<? extends AnnotationValue> vals, List<String> values) {
      for (AnnotationValue val : vals) {
        visit(val, values);
      }
      return null;
    }
  }
}
