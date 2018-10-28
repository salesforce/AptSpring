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
package com.salesforce.apt.graph.processing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.salesforce.apt.graph.model.DefinitionModel;
import com.salesforce.apt.graph.model.errors.ErrorModel;
import com.salesforce.apt.graph.model.errors.ErrorType;
import com.salesforce.apt.graph.model.storage.DefinitionModelStore;

public class DefinitionJoiner {
  
  /**
   * Mutates DefinitionModels, links them to their dependencies.
   * 
   * @param definitions definitions to join.
   * @param store a means to lookup and store definitions that have been, and have just completed being processed.
   * @param el error listeners, pass error models to it to report them to users.
   */
  public void joinDefinitions(Collection<DefinitionModel> definitions,
      DefinitionModelStore store, Consumer<ErrorModel> el) {
    
    //Map of identity to models for passed in Definitions
    Map<String, DefinitionModel> idToDefinition = definitions.stream()
        .collect(Collectors.toMap(dm -> dm.getIdentity(), Function.identity()));
    
    //Function that merges the available models pre-computed on the system with the models found during processing.
    Function<String, List<DefinitionModel>> availableModels = s -> {
      DefinitionModel definied = idToDefinition.get(s);
      if (definied != null) {
        return Arrays.asList(definied); //it was asked to be processed in this build.
      }
      if (store == null || store.lookup(s) == null) {
        return null;
      }
      return store.lookup(s).stream().filter(item -> item != null).collect(Collectors.toList());
    };
    
    for (DefinitionModel model : definitions) {
      errorIfModelBeingProcessedExistsMoreThanOnce(model, availableModels, el);
      errorModelIfMissingOrDupDependencies(model, availableModels, el);
    }    
  }

  /**
   * Looks for depends of the model in available models in the availableModels function (data source)
   * Register errors if available models are too many, too few, or if the array returned is null
   * we assume a read error occurred.
   * 
   * @param model who's dependencies we'll look up
   * @param availableModels a function returning all models with the same id as a list
   * @param el where we register errors.
   */
  private void errorModelIfMissingOrDupDependencies(DefinitionModel model, 
      Function<String, List<DefinitionModel>> availableModels, Consumer<ErrorModel> el) {
    List<String> missingDependencies = new ArrayList<>();
    for (String dependencyIdentity : model.getDependencyNames()) {
      List<DefinitionModel> options = availableModels.apply(dependencyIdentity);
      if (options == null) {
        el.accept(new ErrorModel(ErrorType.COULD_NOT_READ,
            Arrays.asList(new DefinitionModel(dependencyIdentity)),
            Arrays.asList(model)));
      } else {
        switch (options.size()) {
          case 0: 
            missingDependencies.add(dependencyIdentity); 
            break;
          case 1:
            if (options.get(0).isRootNode()) {
              el.accept(new ErrorModel(ErrorType.ROOT_NODE_IMPORTED,
                  Arrays.asList(options.get(0)),
                  Arrays.asList(model)));
            }
            model.addDependency(options.get(0));
            break;
          default:
            el.accept(new ErrorModel(ErrorType.DUPLICATED_MATCHING_DEPENDENCIES,
                availableModels.apply(dependencyIdentity),
                Arrays.asList(model)));
            break;
        }
      }
    }
    if (missingDependencies.size() > 0) {
      el.accept(new ErrorModel(ErrorType.MISSING_NECESSARY_ANNOTATIONS,
            missingDependencies.stream()
              .map(s -> new DefinitionModel(s)).collect(Collectors.toList()),
            Arrays.asList(model)));
    }
  }

  /**
   * Checks whether the model has a conflicting source of data, if so, errors out, 
   * if the array returned by availableModels is null by we assume a read error occurred.
   * 
   * @param model in question
   * @param availableModels a function returning all models with the same id as a list
   * @param el where we register errors.
   */
  private void errorIfModelBeingProcessedExistsMoreThanOnce(DefinitionModel model, Function<String, 
      List<DefinitionModel>> availableModels, Consumer<ErrorModel> el) {
    List<DefinitionModel> allModelsOfIdentity =  availableModels.apply(model.getIdentity());
    if (allModelsOfIdentity == null) {
      el.accept(new ErrorModel(ErrorType.COULD_NOT_READ,
          Arrays.asList(new DefinitionModel(model.getIdentity())),
          Arrays.asList(model)));
    } else {
      if (allModelsOfIdentity.size() > 1) {
        el.accept(new ErrorModel(ErrorType.DUPLICATED_MATCHING_DEFINITIONS,
            allModelsOfIdentity,
            Arrays.asList(model)));
      }
    }
  }
}
