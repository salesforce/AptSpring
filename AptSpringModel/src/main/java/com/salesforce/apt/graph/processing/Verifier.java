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
package com.salesforce.apt.graph.processing;

import java.util.Collection;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

import com.salesforce.apt.graph.model.DefinitionModel;
import com.salesforce.apt.graph.model.errors.ErrorModel;
import com.salesforce.apt.graph.model.storage.DefinitionModelStore;
import com.salesforce.apt.graph.types.AssignabilityUtils;

public class Verifier {
  
  public static class ErrorListener implements Consumer<ErrorModel> {
    
    private ConcurrentLinkedQueue<ErrorModel> errors = new ConcurrentLinkedQueue<>();
    
    private volatile boolean locked = false;
    
    @Override
    public void accept(ErrorModel t) {
      if (locked) {
        throw new IllegalStateException("This error listener's contents have already been fetched, may not add new errors");
      }
      errors.add(t);
    }

    public boolean hasError() {
      return !errors.isEmpty();
    }
    
    public Queue<ErrorModel> getErrors() {
      locked = true;
      return errors;
    }
    
  }
  
  /**
   * Verified the definition model.
   * 
   * @param definitions found definitions in this processing
   * @param assignabilityUtils used to determine if the types are assignable (used to verify injection points). 
   * @param store already processed definitions stored as files.
   * @return any errors found during the verification.  Note may not include all possible errors, as short 
   *     circuit logic is necessary in some places.
   */
  public Queue<ErrorModel> verifyDefinitions(
      Collection<DefinitionModel> definitions, 
      AssignabilityUtils assignabilityUtils,
      DefinitionModelStore store) {
    ErrorListener el = new ErrorListener();
    verifyDefinitions(definitions, store, el, assignabilityUtils);
    return el.getErrors();
  }
  
  /**
   * Verified the definition model.  Registers all errors with the passed in {@link ErrorListener}.
   * 
   * @param definitions found definitions in this processing
   * @param supplier already processed definitions stored as files.
   * @return any errors found during the verification.  Note may not include all possible errors, as short 
   *     circuit logic is necessary in some places.
   */
  private void verifyDefinitions(
      Collection<DefinitionModel> definitions,
      DefinitionModelStore store,
      ErrorListener el,
      AssignabilityUtils assignabilityUtils) {

    /*
     * Takes disjoint definitions that have recently been computed from scanning files and compares them against
     * links them to each other in to a directed graph of definitions to dependencies.   The supplier will load
     * pre-computed definitions from the file system.  No further processing is needed on the pre-computed and loaded
     * DefinitionModels.
     * 
     * This is stage 2 of the DefinitionModel's life cycle
     */
    new DefinitionJoiner().joinDefinitions(definitions, store, el);

    //short circuit.
    if (el.hasError()) {
      return;
    }
    
    
    
    /*
     * Verifies that the directed graph is actually a directed acyclic graph.
     * 
     * Note that definition heads may not include the full graph of Definitions, pre-computed DefinitionModels will
     * be loaded from files, and will truncate the graph at many locations.
     * 
     * This method doesn't mutate state in any way, aside from accessing data in the definition graph that will cause
     * the will cause the lockedDefintionsMerged flag to be set to true in all definitions passed in, and the 
     * potential generation of errors
     */
    Set<DefinitionModel> definitionHeads = new DefinitionGraphInpector().inspectDefinitionGraph(definitions, el);

    //short circuit.
    if (el.hasError()) {
      return;
    }
  
    /*
     * Check the expected entities are listed and types are correct for supplied object by usage.
     */
    new DefinitionContentInspector().inspectDefinitionGraph(definitionHeads, el, assignabilityUtils, store);

  }

}
