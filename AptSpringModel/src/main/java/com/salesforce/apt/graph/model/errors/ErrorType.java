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
package com.salesforce.apt.graph.model.errors;

public enum ErrorType {  
  CYCLE_IN_DEFINITION_SOURCES(true),
  CYCLE_IN_OBJECT_DEFINITIONS(true),
  DUPLICATE_OBJECT_DEFINITIONS(false),
  NONLITERAL_STATIC_MEMBER_VARIABLES(false),
  KNOWN_DAMAGING_CLASS(false),
  MISSING_BEAN_DEFINITIONS(false),
  MISSING_NECESSARY_ANNOTATIONS(false),
  UNMATCHED_TYPES(false),
  NO_MATCHING_DEFINITIONS(false),
  DUPLICATED_MATCHING_DEFINITIONS(false),
  DUPLICATED_MATCHING_DEPENDENCIES(false),
  UNUSED_EXPECTED(false),
  COULD_NOT_STORE(false),
  COULD_NOT_READ(false),
  DEPENDENCY_SHA_MISMATCH(false),
  ROOT_NODE_IMPORTED(false);
  
  private boolean cycle;
  
  private ErrorType(boolean cycle) {
    this.cycle = cycle;
  }
  
  public boolean isCycle() {
    return cycle;
  }
}
