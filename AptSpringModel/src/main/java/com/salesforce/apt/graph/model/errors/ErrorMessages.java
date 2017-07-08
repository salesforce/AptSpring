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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ErrorMessages {

  private final Map<ErrorType, String> errorMessages = new HashMap<>();
  
  public String getMessage(ErrorType type) {
    return errorMessages.get(type);
  }
  
  public static Builder builder() {
    return new Builder();
  }
  
  public static class Builder {
    
    private final Map<ErrorType, String> errorMessages = new HashMap<>();
    
    public Builder cycleInDefinitionSources(String message) {
      errorMessages.put(ErrorType.CYCLE_IN_DEFINITION_SOURCES, message);
      return this;
    }
    
    public Builder cycleInObjectDefinitions(String message) {
      errorMessages.put(ErrorType.CYCLE_IN_OBJECT_DEFINITIONS, message);
      return this;
    }

    public Builder duplicateObjectDefinitions(String message) {
      errorMessages.put(ErrorType.DUPLICATE_OBJECT_DEFINITIONS, message);
      return this;
    }

    public Builder nonLiteralStaticMemberVariables(String message) {
      errorMessages.put(ErrorType.NONLITERAL_STATIC_MEMBER_VARIABLES, message);
      return this;
    }

    public Builder knownDamagingClass(String message) {
      errorMessages.put(ErrorType.KNOWN_DAMAGING_CLASS, message);
      return this;
    }

    public Builder missingBeanDefinitions(String message) {
      errorMessages.put(ErrorType.MISSING_BEAN_DEFINITIONS, message);
      return this;
    }
    
    public Builder missingRelevantAnnotations(String message) {
      errorMessages.put(ErrorType.MISSING_NECESSARY_ANNOTATIONS, message);
      return this;
    }

    public Builder unmatchedTypes(String message) {
      errorMessages.put(ErrorType.UNMATCHED_TYPES, message);
      return this;
    }

    public Builder unusedExpected(String message) {
      errorMessages.put(ErrorType.UNUSED_EXPECTED, message);
      return this;
    }
    
    public Builder noMatchingDefinition(String message) {
      errorMessages.put(ErrorType.NO_MATCHING_DEFINITIONS, message);
      return this;
    }
    
    public Builder duplicatedMatchingDependencies(String message) {
      errorMessages.put(ErrorType.DUPLICATED_MATCHING_DEPENDENCIES, message);
      return this;
    }
    
    public Builder duplicatedMatchingDefinitions(String message) {
      errorMessages.put(ErrorType.DUPLICATED_MATCHING_DEFINITIONS, message);
      return this;
    }
    

    public Builder couldNotStore(String message) {
      errorMessages.put(ErrorType.COULD_NOT_STORE, message);
      return this;
    }

    public Builder couldNotRead(String message) {
      errorMessages.put(ErrorType.COULD_NOT_READ, message);
      return this;
    }
    
    public Builder dependencyShaMismatch(String message) {
      errorMessages.put(ErrorType.DEPENDENCY_SHA_MISMATCH, message);
      return this;
    }

    public Builder rootNodeImported(String message) {
      errorMessages.put(ErrorType.ROOT_NODE_IMPORTED, message);
      return this;
    }
    
    public ErrorMessages build() {
      verifyAllSet();
      ErrorMessages output = new ErrorMessages();
      output.errorMessages.putAll(errorMessages);
      return output;
    }
    
    private void verifyAllSet() {
      List<ErrorType> missing =  new ArrayList<>(Arrays.asList(ErrorType.values()));
      missing.removeAll(errorMessages.keySet());
      if (missing.size() != 0) {
        throw new RuntimeException("Missing error messages for: " + missing); 
      }
    }
  }
  
}
