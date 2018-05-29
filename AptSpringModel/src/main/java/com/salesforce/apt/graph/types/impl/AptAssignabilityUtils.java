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
package com.salesforce.apt.graph.types.impl;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import com.salesforce.apt.graph.model.InstanceModel;
import com.salesforce.apt.graph.naming.NamingTools;
import com.salesforce.apt.graph.types.AssignabilityUtils;

/**
 * Compares types stored in the DefinitionGraph with the best available utils, Types and Elements in the case of apt.
 */
public class AptAssignabilityUtils implements AssignabilityUtils {

  private Types typeUtils;
  private Elements elementUtils;
  
  public AptAssignabilityUtils(Types types, Elements elements) {
    typeUtils = types;
    elementUtils = elements;
  }
  
  /**
   * Given two instances models, the subject and target, it is determined
   * if the subject can be assigned to the dependency on the subject, from the
   * target.
   *
   * @param subject instance to be injected in to the target
   * @param target instance to have the subject injected in to it.
   * @return whether the target can be safely injected in to the correct parameter of the target.
   */
  public boolean isAssignableFrom(InstanceModel subject, InstanceModel target) {
    ExecutableElement factoryOrConstructor = lookUpElement(subject);
    TypeMirror subjectElementType = null;
    if ("<init>".equals(factoryOrConstructor.getSimpleName().toString())) {
      subjectElementType = factoryOrConstructor.getEnclosingElement().asType();
    } else {
      subjectElementType = factoryOrConstructor.getReturnType();
    }
    int count = 0;
    while (!target.getDependencies().get(count).getIdentity().equals(subject.getIdentity())) {
      count ++;
    }
    TypeMirror targetElementType = lookUpElement(target).getParameters().get(count).asType();
    return typeUtils.isAssignable(subjectElementType, targetElementType);
  }
  
  /**
   * Find's the executable element that will have the necessary type
   * information extracted from it.
   * 
   * @param target the instance model that is our target
   * @return the element in question
   */
  public ExecutableElement lookUpElement(final InstanceModel target) {
    //Prior I had used the cached instances of the source element, since idea reuses compiler instances, 
    //and hence AptProcessor instances that proves dangerous as the Type references for the same class/type
    //are not portable across different compilation rounds.
    //
    // Leaving this as a warning to my future self.
    //
    //if (target.getSourceElement().isPresent()) {
    //  return (ExecutableElement) target.getSourceElement().get();
    //} else {
    TypeElement type = elementUtils.getTypeElement(target.getOwningDefinition().replace('$', '.'));
    final NamingTools names = new NamingTools();
    return type.getEnclosedElements().stream().filter(e -> 
        names.elementToName(e).equals(target.getElementLocation()) && ExecutableElement.class.isAssignableFrom(e.getClass()))
          .findFirst()
          .map(e -> (ExecutableElement) e)
          .get();
  }  
  
  /* If the above doesn't work, this handles all but ?, &, | in types.
   * 
  public boolean isAssignableFrom(String subject, String target) {
    ParseType subjectType = ParseType.parse(subject);
    ParseType targetType = ParseType.parse(target);
    return typeUtils.isAssignable(from(subjectType),
        from(targetType));
  }
  
  public TypeMirror from(ParseType parsed) {
    return typeUtils.getDeclaredType(elementUtils.getTypeElement(parsed.getType()),
        parsed.getParameters().stream().map(p -> from(p)).toArray(i -> new TypeMirror[i]));
  }
  */
}
