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
package com.salesforce.apt.graph.model.storage.classpath;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.security.DigestInputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.salesforce.apt.graph.model.DefinitionModel;
import com.salesforce.apt.graph.model.storage.DefinitionModelStore;
import com.salesforce.apt.graph.model.storage.DefinitionOutputStreamProvider;
import com.salesforce.apt.graph.model.storage.Resource;
import com.salesforce.apt.graph.model.storage.ResourceLoader;

public class GsonDefinitionModelStore implements DefinitionModelStore {
  
  private final ResourceLoader resourceLocator;
  
  private final DefinitionOutputStreamProvider definitionModelToStore;

  private final Gson gson;
  
  protected Gson getGson() {
    return gson;
  }

  protected DefinitionOutputStreamProvider getDefinitionOutputStreamProvider() {
    return definitionModelToStore;
  }

  protected ResourceLoader getResourceLocator() {
    return resourceLocator;
  }
  
  public GsonDefinitionModelStore(ResourceLoader resourceLocator,
      DefinitionOutputStreamProvider definitionModelToStore) {
    gson = new GsonBuilder().setPrettyPrinting().create();
    this.resourceLocator = resourceLocator; 
    this.definitionModelToStore = definitionModelToStore;
  }
  
  /**
   * Find definitions by name.
   * 
   * @param name the name of the definition to find.
   * @return A list of all definitions that happen to have the name (from multiple jars?)
   */
  @Override
  public List<DefinitionModel> lookup(String name) {
    List<DefinitionModel> output = new ArrayList<>();
    for (Resource resource : resourceLocator.getEntries(name)) {    
      try (DigestInputStream digestInputStream = new DigestInputStream(resource.getInputStream(), getSha256Digest()); 
          Reader reader = new InputStreamReader(digestInputStream, StandardCharsets.UTF_8)) {
        DefinitionModel definitionModel = gson.fromJson(reader, DefinitionModel.class);
        definitionModel.setSourceLocation(resource.getLocation());
        definitionModel.setSha256(bytesToHex(digestInputStream.getMessageDigest().digest()));
        output.add(definitionModel);
      } catch (IOException ex) {
        return null;
      }
    }
    return output;
  }
 
  /**
   * Hex encoded bytes from the input array. presented as a String 
   * @param bytes to convert
   * @return String of hex values representing the byte array.
   */
  public static String bytesToHex(byte[] bytes) {
    final char[] hexArray = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
    char[] hexChars = new char[bytes.length * 2];
    for (int j = 0; j < bytes.length; j++) {
      int value = bytes[j] & 0xFF;
      hexChars[j * 2] = hexArray[value >>> 4];
      hexChars[j * 2 + 1] = hexArray[value & 0x0F];
    }
    return new String(hexChars);
  }

  @Override
  public boolean store(DefinitionModel model) {
    OutputStream stream = null;
    try {
      stream = definitionModelToStore.store(model);
      DigestOutputStream digesterStream = new DigestOutputStream(stream, getSha256Digest());
      try (OutputStreamWriter writer = new OutputStreamWriter(digesterStream, StandardCharsets.UTF_8)) {
        gson.toJson(model, writer);
      }
      digesterStream.flush();
      model.setSha256(bytesToHex(digesterStream.getMessageDigest().digest()));

      //when in incremental mode, the underlying streams throw exceptions if closed multiple times.
      try {
        digesterStream.close();
      } catch (IOException ioe) {
        //prevent m2e not letting the underlying stream be closed multiple times from
        //causing an IOException that would cause storage to appear to break.
      }
      try {
        stream.close();
      } catch (IOException ioe) {
        //prevent m2e not letting the underlying stream be closed multiple times from
        //causing an IOException that would cause storage to appear to break.
      }
      return true;
    } catch (IOException ex) {
      //indicate that the file could not be stored.
      return false;
    }
  }
  
  /**
   * Get the standard SHA-256 message digest.   Every implementation of MessageDigest must
   * provide SHA-256 message digest
   * @return SHA-256 message digest
   */
  protected MessageDigest getSha256Digest() {
    try {
      return MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException nsae) {
      throw new IllegalStateException("Your jvm doesn't implement the default MessageDigesters... namely sha256.  Fail.");
    }
  }
}
