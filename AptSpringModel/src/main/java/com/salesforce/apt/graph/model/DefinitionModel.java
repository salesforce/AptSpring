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
package com.salesforce.apt.graph.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.TypeElement;

public class DefinitionModel extends AbstractModel {

  private final List<InstanceModel> objectDefinitions = new ArrayList<>();
  private final List<ExpectedModel> expectedDefinitions = new ArrayList<>();
  private final List<String> dependencyNames = new ArrayList<>();
  
  //because this field is transient, deserializers will not set it.  We must
  //carefully account for this by setting the value anywhere it may be used.
  private transient List<DefinitionModel> dependencies;
  private final List<InstanceModel> providedInstances = new ArrayList<>();
  private final Set<ExpectedModel> computedExpected = new HashSet<>();
  private String sha256;          //when read from/written to file.
  private String sourceLocation;  //when read from file
  private final boolean rootNode;
  
  private final Map<String, String> dependencyNameToSha256 = new HashMap<>();  
  
  private transient boolean lockedSourceRead = false;                  //phase 1
  private transient boolean lockedDefintionsMerged = false;            //phase 2
  private transient boolean lockedAnalyzed = false;                    //phase 3

  private void failIfLockRead() {
    if (isLockedSourceRead()) {
      throw new IllegalStateException("Attempting to modify 'source read' content after source read is locked");
    }
  }
  
  private void failIfDefintionsMerged() {
    lockSourceRead();
    if (isLockedDefintionsMerged()) {
      throw new IllegalStateException("Attempting to modify 'definition merge' content after definition merge is locked");
    }
  }
  
  private void failIfLockedAnalyzed() {
    lockDefintionsMerged();
    if (isLockedAnalyzed()) {
      throw new IllegalStateException("Attempting to modify analyzed structure after structure has been computed");
    }
  }
  
  /**
   * @return true if this is a root node, and no other nodes may depend on it.
   */
  public boolean isRootNode() {
    return rootNode;
  }
  
  private void setDepenendencyArrayIfNull() {
    if (dependencies == null) {
      dependencies = new ArrayList<>();
    }
  }
  
  public List<DefinitionModel> getDependencies() {
    setDepenendencyArrayIfNull();
    lockDefintionsMerged();
    return Collections.unmodifiableList(dependencies);
  }

  public DefinitionModel(String name) {
    this(name, false);
  }

  public DefinitionModel(String name, boolean rootNode) {
    super(name);
    this.rootNode = rootNode;
  }
  
  public DefinitionModel(TypeElement type) {
    this(type, false);
  }
  
  public DefinitionModel(TypeElement type, boolean rootNode) {
    super(type);
    this.rootNode = rootNode;
  }

  public void addDependency(DefinitionModel model) {
    setDepenendencyArrayIfNull();
    failIfDefintionsMerged();
    this.dependencies.add(model);
  }

  public void addDependencies(List<DefinitionModel> model) {
    setDepenendencyArrayIfNull();
    failIfDefintionsMerged();
    this.dependencies.addAll(model);
  }

  public void addDefinition(InstanceModel model) {
    failIfLockRead();
    this.objectDefinitions.add(model);
  }

  public void addDefinition(ExpectedModel model) {
    failIfLockRead();
    this.expectedDefinitions.add(model);
  }

  public void addDependencyNames(List<String> model) {
    failIfLockRead();
    this.dependencyNames.addAll(model);
  }
  
  public void addDependencyNames(String dependencyName) {
    failIfLockRead();
    this.dependencyNames.add(dependencyName);
  }

  public List<String> getDependencyNames() {
    lockSourceRead();
    return Collections.unmodifiableList(dependencyNames);
  }
  
  public List<InstanceModel> getObjectDefinitions() {
    lockSourceRead();
    return Collections.unmodifiableList(objectDefinitions);
  }

  public List<ExpectedModel> getExpectedDefinitions() {
    lockSourceRead();
    return Collections.unmodifiableList(expectedDefinitions);
  }

  public boolean isComplete() {
    return getExpectedDefinitions().size() == 0;
  }
  
  public List<InstanceModel> getProvidedInstances() {
    lockAnalyzed();
    return Collections.unmodifiableList(providedInstances);
  }
  
  public void addAllProvidedInstances(Collection<InstanceModel> providedInstances) {
    failIfLockedAnalyzed();
    this.providedInstances.addAll(providedInstances);
  }

  public Set<ExpectedModel> getComputedExpected() {
    lockAnalyzed();
    return Collections.unmodifiableSet(computedExpected);
  }
  
  public void addAllComputedExpected(Collection<ExpectedModel> computedExpected) {
    failIfLockedAnalyzed();
    this.computedExpected.addAll(computedExpected);
  }
  
  public void addComputedExpected(ExpectedModel computedExpected) {
    failIfLockedAnalyzed();
    this.computedExpected.add(computedExpected);
  }
  
  /**
   * For the purposes of identification of the object definition class, the location of the class suffices.
   *
   * @return a unique identity representing this definition, specifically the fully qualified name of the source type.
   */
  public String getIdentity() {
    //wont lockSourceRead(); as identity is read while attaching definitions.
    return getElementLocation();
  }

  /**
   * For the purposes of identification of the object definition class, the location of the class suffices.
   *
   * @return the Identity of the {@link DefinitionModel}
   */
  public String toString() {
    lockSourceRead();
    return getElementLocation();
  }

  
  public String getSha256() {
    lockAnalyzed();
    return sha256;
  }

  public void setSha256(String sha256) {
    failIfLockedAnalyzed();
    lockAnalyzed();
    this.sha256 = sha256;
  }

  public boolean isLockedSourceRead() {
    return lockedSourceRead;
  }
  
  public boolean isLockedDefintionsMerged() {
    return lockedDefintionsMerged;
  }

  public boolean isLockedAnalyzed() {
    return lockedAnalyzed;
  }

  private void lockSourceRead() {
    lockedSourceRead = true;
  }

  private void lockDefintionsMerged() {
    lockSourceRead();
    lockedDefintionsMerged = true;
  }

  /**
   * Public so that once all shas are write to {@link DefinitionModel#addDependencyNameToSha256} this 
   * can be called, locking down the definition.
   */
  private void lockAnalyzed() {
    lockSourceRead();
    lockDefintionsMerged();
    lockedAnalyzed = true;
  }

  
  public Map<String, String> getDependencyNameToSha256() {
    lockDefintionsMerged();
    return Collections.unmodifiableMap(dependencyNameToSha256);
  }

  public void addDependencyNameToSha256(String dependencyName, String sha256) {
    failIfLockedAnalyzed();
    dependencyNameToSha256.put(dependencyName, sha256);
  }

  public void addAllDependencyNameToSha256(Map<String, String>  dependencyNameToSha256) {
    failIfLockedAnalyzed();
    dependencyNameToSha256.putAll(dependencyNameToSha256);
  }

  public String getSourceLocation() {
    return sourceLocation;
  }

  public void setSourceLocation(String sourceLocation) {
    failIfLockRead();
    this.sourceLocation = sourceLocation;
  }

  public String getSourcePackage() {
    String packageName = "";
    if (getIdentity().lastIndexOf(".") != -1) {
      packageName = getIdentity().substring(0, getIdentity().lastIndexOf("."));
    }
    return packageName;
  }

  public String getSourceClass() {
    return getIdentity().substring(getIdentity().lastIndexOf(".") + 1);
  } 
}
