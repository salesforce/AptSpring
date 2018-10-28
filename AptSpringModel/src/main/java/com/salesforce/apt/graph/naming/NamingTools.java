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
package com.salesforce.apt.graph.naming;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;

public class NamingTools {
  
  /*
  public String elementToName(Element element) {
    switch (element.getKind()) {
      case CLASS:
        return element.toString();
      case METHOD:
        return element.getEnclosingElement().toString() + "." + element.toString();
      case CONSTRUCTOR:
        return element.getEnclosingElement().toString() + "." + element.toString();
      default:
        return element.toString();
    }
  }*/

  public String elementToName(Element element) {
    return elementToFileName(element);
  }

  /**
   * Creates a unique name (suitable for storage) from an element.
   * Note that nested class names must be delimited by '$' so that
   * they may be used as filenames.
   * 
   * @param element to turn in to a name.
   * @return a uniquely identifying string.
   */
  public String elementToFileName(Element element) {
    switch (element.getKind()) {
      //case PACKAGE:
      //  //so looks like there is difference between jdt and jdk here.... sigh.
      //  //jdk returns the full package name on toString, jdt returns on the last part.
      //  return element.asType().toString();  
      case CLASS:
        return  calculateClassName(element,  element.getSimpleName().toString());
      case METHOD:
        return elementToFileName(element.getEnclosingElement()) + "." + element.toString();
      case CONSTRUCTOR:
        return elementToFileName(element.getEnclosingElement()) + "." + element.toString();
      default:
        return element.toString();
    }
  }
  
  private String calculateClassName(Element element, String current) {
    Element enclosing = element.getEnclosingElement();
    if (enclosing.getKind() == ElementKind.PACKAGE) {
      return element.toString().substring(0, element.toString().lastIndexOf(".")) + "." + current;
    }
    if (enclosing.getKind() == ElementKind.CLASS) {
      return calculateClassName(element.getEnclosingElement(), enclosing.getSimpleName().toString() + "$" + current);
    }
    return element.toString();
  }

}
