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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;

import org.junit.Test;

public class TestTypeParse {

  private static final String JAVA_LANG_INTEGER = "java.lang.Integer";
  private static final String JAVA_UTIL_MAP = "java.util.Map";
  private static final String JAVA_UTIL_LIST = "java.util.List";
  private static final String JAVA_LANG_STRING = "java.lang.String";

  @Test
  public void testString() {
    String name = JAVA_LANG_STRING;
    ParseType expectedType = new ParseType(name);
    ParseType calc = ParseType.parse(name);
    assertThat(ParseType.parse(name)).isEqualTo(expectedType);
    assertThat(calc.hashCode()).isEqualTo(expectedType.hashCode());
    assertThat(calc).isEqualTo(expectedType);
  }

  @Test
  public void testListString() {
    String name = "java.util.List<java.lang.String>";
    ParseType expectedType = new ParseType(JAVA_UTIL_LIST, new ParseType(JAVA_LANG_STRING));
    ParseType calc = ParseType.parse(name);
    assertThat(calc).isEqualTo(expectedType);
    assertThat(calc.toString()).isEqualTo(name);
    assertThat(calc.hashCode()).isEqualTo(expectedType.hashCode());
  }

  @Test
  public void testMapStringInt() {
    String name = "java.util.Map<java.lang.String,java.lang.Integer>";
    ParseType expectedType = new ParseType(JAVA_UTIL_MAP, new ParseType(JAVA_LANG_STRING), new ParseType(JAVA_LANG_INTEGER));
    ParseType calc = ParseType.parse(name);
    assertThat(calc).isEqualTo(expectedType);
    assertThat(calc.toString()).isEqualTo(name);
    assertThat(calc.hashCode()).isEqualTo(expectedType.hashCode());
  }

  @Test
  public void testMapStringListInt() {
    String name = "java.util.Map<java.lang.String,java.util.List<java.lang.Integer>>";
    ParseType expectedType = new ParseType(JAVA_UTIL_MAP, new ParseType(JAVA_LANG_STRING),
        new ParseType(JAVA_UTIL_LIST, new ParseType(JAVA_LANG_INTEGER)));
    ParseType calc = ParseType.parse(name);
    assertThat(calc).isEqualTo(expectedType);
    assertThat(calc.toString()).isEqualTo(name);
    assertThat(calc.hashCode()).isEqualTo(expectedType.hashCode());
  } 

  @SuppressWarnings("unlikely-arg-type")
  @Test
  public void testEquality() {
    String name = "java.util.Map<java.lang.String,java.util.List<java.lang.Integer>>";
    ParseType complex = ParseType.parse(name);
    assertThat(complex.equals(null)).isFalse();
    assertThat(complex.equals(complex)).isTrue();
    assertThat(complex.equals(new ArrayList<>())).isFalse();
    ParseType nullType = new ParseType(null);
    ParseType nullType2 = new ParseType(null);
    assertThat(nullType.equals(nullType2)).isTrue();
    assertThat(nullType.hashCode()).isEqualTo(nullType2.hashCode());
    assertThat(complex.equals(nullType)).isFalse();
    assertThat(nullType.equals(complex)).isFalse(); 
    assertThat(nullType.equals(new ParseType("test1"))).isFalse();    
    assertThat(new ParseType("test1").equals(new ParseType("test2"))).isFalse();    
  }

}
