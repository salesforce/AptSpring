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
package com.salesforce.apt.graph.model.errors;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.salesforce.apt.graph.model.AbstractModel;

public class ErrorModel {

  private final ErrorType message;
  private final List<AbstractModel> causes;
  private final List<AbstractModel> involved;
  
  public ErrorType getMessage() {
    return message;
  }

  public List<AbstractModel> getCauses() {
    return causes;
  }
  
  public List<AbstractModel> getInvolved() {
    return involved;
  }
  
  public boolean isCyclic() {
    return message.isCycle();
  }
  
  public String toString() {
    return message.name() + " on " + involved.toString() + " caused by " + causes;
  }
  
  /**
   * A non cyclic error
   * @param message {@link ErrorType} of this represents
   * @param causes models at fault.
   * @param involved models the error should be displayed on.
   */
  public ErrorModel(ErrorType message, List<? extends AbstractModel> causes, List<? extends AbstractModel> involved) {
    super();
    this.message = message;
    if (message.isCycle() && !causes.equals(involved)) {
      throw new IllegalArgumentException("Malformed ErrorModel");
    }
    this.causes = Collections.unmodifiableList(causes);
    this.involved = Collections.unmodifiableList(involved);   
  }
  
  /**
   * Returns a contextualized error message for the error, from the model's perspective.
   * 
   * @param on an AbstractModel that is "involved" in this error
   * @param converter presents an error message for this error model to be displayed on the AbstractModel, on,
   *     that contributed to the error.
   * @return an error message from the context of the failed model member.
   */
  public String getMessageOn(AbstractModel on, Function<ErrorType, String> converter) {
    if (!isCyclic()) {
      if (getCauses().size() == 1) {
        return MessageFormat.format(converter.apply(getMessage()), getCauses().get(0));
      } else {
        return MessageFormat.format(converter.apply(getMessage()), 
            getCauses().stream().filter(m -> !m.equals(on)).map(m -> m.toString()).collect(Collectors.joining(", "))); 
      }
    }
    StringBuilder builder = new StringBuilder();
    int index = getInvolved().indexOf(on);
    if (index != -1) {
      for (int i = index; i < getInvolved().size(); i++) {
        builder.append(getInvolved().get(i).toString());
        if (i + 1 < getInvolved().size() || index != 0) {
          builder.append(" -> ");
        }
      }
      for (int i = 0; i < index; i++) {
        builder.append(getInvolved().get(i).toString());
        if (i + 1 < index) {
          builder.append(" -> ");
        }
      }
    }
    return MessageFormat.format(converter.apply(getMessage()), builder.toString());
  }

}
