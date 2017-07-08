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
package com.salesforce.apt.graph.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import com.salesforce.apt.graph.model.DefinitionModel;
import com.salesforce.apt.graph.model.errors.ErrorMessages;
import com.salesforce.apt.graph.model.errors.ErrorModel;
import com.salesforce.apt.graph.model.storage.DefinitionModelStore;
import com.salesforce.apt.graph.processing.Verifier;
import com.salesforce.apt.graph.types.AssignabilityUtils;

public class ParsingContext {

  private final ErrorMessages messageFormats;

  protected final List<DefinitionModel> definitions = new ArrayList<>();
    
  protected final AssignabilityUtils assignabilityUtils;
  
  protected final DefinitionModelStore store;
  
  public ErrorMessages getMessageFormats() {
    return messageFormats;
  }
  
  public ParsingContext(ErrorMessages messageFormats,
      AssignabilityUtils assignabilityUtils, DefinitionModelStore store) {
    this.messageFormats = messageFormats;
    this.assignabilityUtils = assignabilityUtils;
    this.store = store;
  }

  public void addDefinition(DefinitionModel definition) {
    this.definitions.add(definition);
  }
  
  public Queue<ErrorModel> checkAndStoreValid() {
    return new Verifier().verifyDefinitions(this.definitions, assignabilityUtils, store);
  }

}
