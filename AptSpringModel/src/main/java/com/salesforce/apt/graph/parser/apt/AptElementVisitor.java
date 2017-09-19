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
package com.salesforce.apt.graph.parser.apt;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.AbstractElementVisitor8;

import com.salesforce.apt.graph.model.DefinitionModel;

public class AptElementVisitor extends AbstractElementVisitor8<Void, AptParsingContext> {

  private DefinitionParser parser;

  public AptElementVisitor(DefinitionParser parser) {
    this.parser = parser;
  }

  protected Void defaultAction(Element element, AptParsingContext definitions) {
    for (Element e1 : element.getEnclosedElements()) {
      this.visit(e1);
    }
    return null;
  }

  @Override
  public Void visitPackage(PackageElement element, AptParsingContext definitions) {
    return defaultAction(element, definitions);
  }

  @Override
  public Void visitType(TypeElement element, AptParsingContext definitions) {
    DefinitionModel def = parser.parseDefinition(element);
    if (def != null) {
      definitions.addDefinition(def);
    }
    return defaultAction(element, definitions);
  }

  @Override
  public Void visitVariable(VariableElement element, AptParsingContext definitions) {
    return defaultAction(element, definitions);
  }

  @Override
  public Void visitExecutable(ExecutableElement element, AptParsingContext definitions) {
    return defaultAction(element, definitions);
  }

  @Override
  public Void visitTypeParameter(TypeParameterElement element, AptParsingContext definitions) {
    return defaultAction(element, definitions);
  }
}
