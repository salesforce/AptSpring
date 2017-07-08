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

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.SimpleElementVisitor8;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic.Kind;

import com.salesforce.apt.graph.model.AbstractModel;
import com.salesforce.apt.graph.model.DefinitionModel;
import com.salesforce.apt.graph.model.errors.ErrorMessages;
import com.salesforce.apt.graph.model.errors.ErrorModel;
import com.salesforce.apt.graph.model.storage.apt.AptFilerStore;
import com.salesforce.apt.graph.model.storage.apt.AptResourceLoader;
import com.salesforce.apt.graph.model.storage.apt.ClassFileGsonDefinitionModelStore;
import com.salesforce.apt.graph.parser.ParsingContext;
import com.salesforce.apt.graph.types.impl.AptAssignabilityUtils;

public class AptParsingContext extends ParsingContext {

  private Elements elementUtils;
  
  public AptParsingContext(ErrorMessages messageFormats,
      Filer filer, Elements elementUtils, Types types) {
    super(messageFormats, new AptAssignabilityUtils(types, elementUtils),
        new ClassFileGsonDefinitionModelStore(new AptResourceLoader(elementUtils), new AptFilerStore(filer)));
    this.elementUtils = elementUtils;
  }

  public void addDefinition(DefinitionModel model) {
    super.addDefinition(model);
  }
  
  /**
   * Gives user feedback as info/warnings/errors during compilation (works in m2e with takari-lifecycle).
   * 
   * @param messager APT round handler for user messages
   */
  public void outputErrors(Messager messager) {
    Iterable<ErrorModel> errors = checkAndStoreValid();
    for (ErrorModel error : errors) {
      for (AbstractModel model : error.getInvolved()) {
        if (model.getSourceElement().isPresent()) {
          if (model.getSourceElement().isPresent()) {
            messager.printMessage(Kind.ERROR, error.getMessageOn(model, k -> getMessageFormats().getMessage(k)),
                getCorrespondingElement(elementUtils, model.getSourceElement().get()));
          }
        } 
      }
    }
  }
  
  
  public Element getCorrespondingElement(Elements elementUtils, Element element) {
    Current current = new Current(elementUtils);
    return current.visit(element);
  }
  
  private static class Current extends SimpleElementVisitor8<Element, Void> {

    private final Elements elementUtils;
    
    public Current(Elements elementUtils) {
      this.elementUtils = elementUtils;
    }
    
    @Override
    public Element visitVariable(VariableElement e, Void p) {
      return super.visitVariable(e, p);
    }

    @Override
    protected Element defaultAction(Element e, Void p) {
      return super.defaultAction(e, p);
    }

    @Override
    public Element visitType(TypeElement e, Void p) {
      return elementUtils.getTypeElement(e.getQualifiedName());
    }

    @Override
    public Element visitExecutable(ExecutableElement e, Void p) {
      TypeElement type = (TypeElement) visitType((TypeElement)e.getEnclosingElement(), null);
      /*
       * Hacky but works for now.  TODO:  build a proper mechanism to convert an executable element to 
       * the current RoundEnv's element representation of the the initial element that was read.
       */
      for (Element element : type.getEnclosedElements()) {
        if (element.toString().equals(e.toString())) {  
          return element;
        }
      }
      return null;
    }

    @Override
    public Element visitUnknown(Element e, Void p) {
      return super.visitUnknown(e, p);
    }
    
  }
  
}
