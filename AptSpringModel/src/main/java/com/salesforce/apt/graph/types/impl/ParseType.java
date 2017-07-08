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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

public class ParseType {

  private final String type;
  private final List<ParseType> parameters = new ArrayList<>();

  public ParseType(String type) {
    this.type = type;
  }
  
  public ParseType(String type, ParseType... parameters) {
    this.type = type;
    this.parameters.addAll(Arrays.asList(parameters));
  }
  
  public String getType() {
    return type;
  }

  public List<ParseType> getParameters() {
    return parameters;
  }
  
  public static ParseType parse(String type) {
    final String parseToken = "*";
    String[] tokens = type
        .replaceAll("<", parseToken + "<" + parseToken)
        .replaceAll(">", parseToken + ">" + parseToken)
        .replaceAll(",", parseToken + "," + parseToken).split("\\" + parseToken);
    Queue<String> tokenQueue = new LinkedList<>(Arrays.asList(tokens).stream().map(t -> t.trim()).collect(Collectors.toList())); 
    return parseParameters(tokenQueue);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + parameters.hashCode();
    result = prime * result + ((type == null) ? 0 : type.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    ParseType other = (ParseType)obj;
    if (!parameters.equals(other.parameters)) {
      return false;
    }
    if (type == null) {
      if (other.type != null) {
        return false;
      }
    } else {
      if (!type.equals(other.type)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder(getType());
    if (!getParameters().isEmpty()) {
      builder.append('<');
      Iterator<ParseType> iter = getParameters().iterator();
      while (iter.hasNext()) {
        builder.append(iter.next().toString());
        if (iter.hasNext()) {
          builder.append(',');
        }
      }
      builder.append('>');
    }
    return builder.toString();
  }

  private static ParseType parseParameters(Queue<String> tokens) {
    ParseType child = new ParseType(tokens.remove());
    if (tokens.isEmpty()) {
      return child;
    }
    if (",".equals(tokens.peek())) {
      return child;
    }
    if ("<".equals(tokens.peek())) {
      tokens.remove();
      child.parameters.add(parseParameters(tokens));
      while (",".equals(tokens.peek())) {
        tokens.remove();
        child.parameters.add(parseParameters(tokens));
      }
      if (">".equals(tokens.peek())) {
        tokens.remove();
      }
    }
    return child;
  }
    
}
