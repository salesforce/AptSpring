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
package com.salesforce.apt.graph.model.storage.apt;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import javax.annotation.Generated;
import javax.lang.model.element.Modifier;

import com.salesforce.apt.graph.model.DefinitionModel;
import com.salesforce.apt.graph.model.storage.DefinitionOutputStreamProvider;
import com.salesforce.apt.graph.model.storage.ResourceLoader;
import com.salesforce.apt.graph.model.storage.classpath.FileStore;
import com.salesforce.apt.graph.model.storage.classpath.GsonDefinitionModelStore;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;

public class ClassFileGsonDefinitionModelStore extends GsonDefinitionModelStore {

  public ClassFileGsonDefinitionModelStore(ResourceLoader resourceLocator, DefinitionOutputStreamProvider definitionModelToStore) {
    super(resourceLocator, definitionModelToStore);
  }

  @Override
  public boolean store(DefinitionModel model) {
    String packageName = model.getSourcePackage();
    String className = model.getSourceClass() + "_" + FileStore.STANDARD.getPath();
    String data = getGson().toJson(model);
    
    FieldSpec fieldSpec = FieldSpec.builder(String.class, AptResourceLoader.FIELD_NAME)
        .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
        .initializer("$S", data)
        .addAnnotation(AnnotationSpec.builder(SuppressWarnings.class).addMember("value", "$S", "unused").build())
        .build();

    TypeSpec classSpec = TypeSpec.classBuilder(className)
        .addModifiers(Modifier.PUBLIC)
        .addField(fieldSpec)
        .addAnnotation(AnnotationSpec.builder(Generated.class).addMember("value", "$S", "SpringApt").build())
        .build();
    
    JavaFile javaFile = JavaFile.builder(packageName, classSpec)
        .build();

    try {
      OutputStream stream = getDefinitionOutputStreamProvider().store(model);
      try {
        stream.write(javaFile.toString().getBytes());
      } finally {
        stream.close();
      }
      model.setSha256(bytesToHex(getSha256Digest().digest(data.getBytes(StandardCharsets.UTF_8))));
      return true;
    } catch (IOException ex) {
      throw new IllegalStateException("Could not store model to class", ex);
    }
  }
}
