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

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jgrapht.DirectedGraph;
import org.jgrapht.alg.cycle.SzwarcfiterLauerSimpleCycles;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import com.salesforce.apt.graph.model.AbstractModel;
import com.salesforce.apt.graph.model.BaseInstanceModel;
import com.salesforce.apt.graph.model.DefinitionModel;
import com.salesforce.apt.graph.model.ExpectedModel;
import com.salesforce.apt.graph.model.InstanceDependencyModel;
import com.salesforce.apt.graph.model.InstanceModel;
import com.salesforce.apt.graph.model.errors.ErrorModel;
import com.salesforce.apt.graph.model.errors.ErrorType;
import com.salesforce.apt.graph.model.storage.DefinitionModelStore;
import com.salesforce.apt.graph.types.AssignabilityUtils;

public class DefinitionContentInspector {

  public void inspectDefinitionGraph(Set<DefinitionModel> definitionGraphHeads,
      Consumer<ErrorModel> errorListener, AssignabilityUtils assignabilityUtils, DefinitionModelStore store) {
    for (DefinitionModel definition : definitionGraphHeads) {
      depthFirstExpectedsInspector(definition, errorListener, assignabilityUtils, store);
    }
  }
  
  public InstanceModel getOneWithSourceElementElseAny(final Collection<InstanceModel> possibilities) {
    return possibilities.stream()
         .filter(im -> im.getSourceElement().isPresent())
         .findAny().orElseGet(() -> possibilities.iterator().next());
  }
  
  /**
   * The the sha 256 of dependencies against the stored data.
   * 
   * @param model model who's dependencies we will inspect
   * @param store the store of all the model data (abstraction, will likely be in memory or class files)
   * @param errorListener if any shas mismatch will report here.
   * @return true if all shas match.
   */
  public boolean verifiedShas(DefinitionModel model, DefinitionModelStore store, Consumer<ErrorModel> errorListener) {
    boolean verified = true;
    for (DefinitionModel dep : model.getDependencies()) {
      if (!dep.getSourceElement().isPresent()  //not recompiling
          && model.getDependencyNameToSha256().containsKey(dep.getIdentity())) { //model already has a sha256 of it
        if (!model.getDependencyNameToSha256().get(dep.getIdentity()).equals(dep.getSha256())) {
          errorListener.accept(new ErrorModel(ErrorType.DEPENDENCY_SHA_MISMATCH,
                Arrays.asList(model, dep),  Arrays.asList(model)));
          verified = false;
        }
      }
    }
    return verified;
  }
  
  
  /**
   * Verify that all expected entities are marked expected.
   * Verify that the types of provided entities satisfy all expected types....
   * <p/>
   * if a child record was freshly process, so too must all parent ( if the md5 has changed ).
   *  
   * @param definition the head of current tree of definitions linked by imports
   * @param errorListener registers all errors found 
   */
  private boolean depthFirstExpectedsInspector(DefinitionModel definition, Consumer<ErrorModel> errorListener,
      AssignabilityUtils assignabilityUtils, DefinitionModelStore store) {
    boolean errored = false;
    for (DefinitionModel dependency : definition.getDependencies()) {
      if (!dependency.isLockedAnalyzed()) {
        errored = depthFirstExpectedsInspector(dependency, errorListener, assignabilityUtils, store) || errored;
        if (errored) {
          return true; // no need to continue.
        }
      } else {
        if (!verifiedShas(dependency, store, errorListener)) {
          errored = true;
        }
      }
    }
    
    if (errored) {
      return errored;
    }
    
    //check that each object has only one source ( could be the imported from a dependency, in a diamond pattern )
    //validate the expected beans are correct.
    Map<String, InstanceModel> resolvedInstances = ensureSingleInstanceOfEachName(definition, errorListener);
    
    //short circuit if dependencies couldn't be resolved.
    if (resolvedInstances == null) {
      return false;
    }
    
    //looks for cycles and unexpected missing entities.
    errored = detectCyclesInEntityGraph(definition, resolvedInstances, errorListener);

    //check types of non-expected dependencies
    errored = checkInstancesTypesInDefinition(definition, resolvedInstances, errorListener, assignabilityUtils) || errored;

    //check all definitions with expected, that each instance expecting a definition can use the supplied.
    errored = checkProvidedSupplyCorrectTypes(definition, resolvedInstances, errorListener, assignabilityUtils) || errored;

    //prune all edged from graph that don't end in an expected

    //store computed expects with all types that must satisfied.
    
    
    
    if (!errored) {
      //store as provided dependencies
      definition.addAllProvidedInstances(resolvedInstances.values());
      for (DefinitionModel dep : definition.getDependencies()) {
        definition.addDependencyNameToSha256(dep.getIdentity(), dep.getSha256());
      }
      //storing will lock a the definition, as will reading.
      if (!store.store(definition)) {
        errorListener.accept(
            new ErrorModel(ErrorType.COULD_NOT_STORE, Arrays.asList(definition), Arrays.asList(definition)));
      }
    }
    
    return errored;
  }

  private ErrorModel errorForMismatchedExpected(final DefinitionModel definition, ExpectedModel computedExpected,
      final Map<String, InstanceModel> nameToEntity, AssignabilityUtils assignabilityUtils) {
    //that which is to fill all the expectedInstance references.
    InstanceModel providedInstance = nameToEntity.get(computedExpected.getIdentity());
    
    //the top level definition model should have declared this an expectedBean - maybe move that error here?
    if (providedInstance == null) {
      return null;
    }
    
    //Instances which expect to use the provided instance
    List<InstanceModel> mistmatchedExpectantEntities = computedExpected.getDefinitionReferenceToType().keySet().stream()
        .map(name -> nameToEntity.get(name))
        .filter(expectantInstance -> !assignabilityUtils.isAssignableFrom(providedInstance, expectantInstance))
        .collect(Collectors.toList());
    
    if (mistmatchedExpectantEntities.size() == 0) {
      return null;
    } else {
      List<InstanceModel> involved = new ArrayList<>();
      involved.add(providedInstance);
      involved.addAll(mistmatchedExpectantEntities);
      return new ErrorModel(ErrorType.UNMATCHED_TYPES, involved, Arrays.asList(definition, providedInstance));
    }
  }
  
  private boolean checkProvidedSupplyCorrectTypes(final DefinitionModel definition, final Map<String, InstanceModel> nameToEntity,
      final Consumer<ErrorModel> errorListner, AssignabilityUtils assignabilityUtils) {
    List<ErrorModel> errors = definition.getDependencies().stream()
        .flatMap(dependency -> dependency.getComputedExpected().stream())
        .filter(expectedModel -> !definition.getExpectedDefinitions().stream()
            .anyMatch(ed -> ed.getIdentity().equals(expectedModel.getIdentity())))
        .map(em -> errorForMismatchedExpected(definition, em, nameToEntity, assignabilityUtils))
        .filter(errorModel -> errorModel != null)
        .collect(Collectors.toList());
    errors.stream().forEach(errorListner);
    return errors.size() > 0;
  }
  
  
  private List<Entry<String, InstanceModel>> getEntryListForNameAndAlias(InstanceModel instanceModel) {
    List<Entry<String, InstanceModel>> output = new ArrayList<>();
    output.add(new SimpleEntry<>(instanceModel.getIdentity(), instanceModel));
    for (String alias : instanceModel.getAliases()) {
      if (!alias.equals(instanceModel.getIdentity())) {
        output.add(new SimpleEntry<>(alias, instanceModel));
      }
    }
    return output;
  }
  
  /**
   * Verify that a single named instance model is correctly identifiable composed of owningDefinition/ElementLocation/Identity.
   * 
   * 
   * @param definition the definition of 
   * @param errorListener accepts and displays all errors produced by analyzing the models.
   * @return returns a map of each instance name to the single model that the has been resolved to. 
   */
  private Map<String, InstanceModel> ensureSingleInstanceOfEachName(DefinitionModel definition,
      Consumer<ErrorModel> errorListener) {
    Map<String, Map<String, InstanceModel>> instancesByNameAndLocationDedupped = 
        definitionToAllInstancesByNameAndSourceLocation(definition);
    
    final Map<String, InstanceModel> resolvedDependencies = new HashMap<>();
    boolean errored = false;    
    for (Entry<String, Map<String, InstanceModel>> entry : instancesByNameAndLocationDedupped.entrySet()) {
      if (entry.getValue().size() == 1) {
        resolvedDependencies.put(entry.getKey(), entry.getValue().values().iterator().next()); //get only InstanceModel.
      } else {
        errored = true;
        errorListener.accept(errorForDuplicateInstanceModels(definition, entry.getValue().values().stream()
            .sorted((i1, i2) -> i1.getElementLocation().compareTo(i2.getElementLocation()))
            .collect(Collectors.toList())));
      }
    }
    return errored ? null : resolvedDependencies;
  }
  
  /**
   * Give a definition model, extract all instances from this definition, and all imported definitions.
   * Group the instances by name, and then map them from source location to InstanceModel.
   *  
   * @param definition model to extract all instance information from, including imported definitions.
   * @return a map of maps, [name -> [sourceLocation -> instanceModel]]
   */
  private Map<String, Map<String, InstanceModel>> definitionToAllInstancesByNameAndSourceLocation(
      DefinitionModel definition) {
    //create a stream of all imported Definition's InstanceModels
    Stream<InstanceModel> imported = definition.getDependencies().stream()
        .map(d -> d.getProvidedInstances()).flatMap(x -> x.stream());
    //merge the stream with all local instance models.
    Stream<InstanceModel> instanceModelStream = Stream.concat(definition.getObjectDefinitions().stream(), imported);

    //map all instances by name and then by source location (due to diamond dependencies in definition imports.
    Map<String, Map<String, InstanceModel>> instancesByNameAndLocationDedupped = instanceModelStream
        .flatMap(instance -> getEntryListForNameAndAlias(instance).stream()) //flat map all alias to entries
        .collect(Collectors.groupingBy(entry -> entry.getKey(), //group by name
            Collectors.mapping(entry -> entry.getValue(), 
                Collectors.toMap(im -> im.getElementLocation(), //by location
                    im -> im, //identity function for first insert
                    //choose the one with source element on merge, if any
                    (im1, im2) -> im1.getSourceElement().isPresent() ? im1 : im2))));
    return instancesByNameAndLocationDedupped;
  }

  /**
   * Given a list of duplicate instance models (same identifier, different source elements) produce a 
   * well formed ErrorModel.
   * 
   * @param definition where the error was first detected.
   * @param causes the list of all instances models from the definition or any of it's imported definitions.
   * @return an well formed ErrorModel of the duplicated beans.
   */
  private ErrorModel errorForDuplicateInstanceModels(DefinitionModel definition, List<InstanceModel> causes) {
    ArrayList<AbstractModel> involved = new ArrayList<>();
    involved.add(definition);
    involved.addAll(causes.stream()
        .filter(instanceModel -> instanceModel.getSourceElement().isPresent())
        .collect(Collectors.toList()));
    return new ErrorModel(ErrorType.DUPLICATE_OBJECT_DEFINITIONS, causes, involved);
  }
  
  /**
   * Inspects the instance graph for cycles, any cycle is printed as an error.   The nameToEntity parameter doesn't list expected
   * instances, any instances that are not found in the nameToInstances map (they are looked for because they are referenced as a
   * dependency by an instance in the map) and are not found by name in the definition's expectedInstances are treated as errors
   * as well.
   * 
   * @param definition definition being processed.  Will uses it's expected list, any instances references as dependencies but
   *     not found, not listed as expected in this DefinitionModel, will be treated as errors.
   * @param nameToEntity name to unique instanceModels, verified before call.
   * @param errorListner accepts and displays all errors produced by analyzing the models
   * @return true if an error occurred, false otherwise
   */
  private boolean detectCyclesInEntityGraph(final DefinitionModel definition, final Map<String, InstanceModel> nameToEntity,
      final Consumer<ErrorModel> errorListener) {
    final Map<String, ExpectedModel> missing = new HashMap<>();
    final DirectedGraph<BaseInstanceModel, DefaultEdge> entityGraph = new DefaultDirectedGraph<>(DefaultEdge.class);
    for (BaseInstanceModel entity : nameToEntity.values()) {
      if (!entityGraph.containsVertex(entity)) {
        entityGraph.addVertex(entity);
      }
      if (InstanceModel.class.isAssignableFrom(entity.getClass())) {
        InstanceModel instanceModel = (InstanceModel) entity;
        for (InstanceDependencyModel instanceDependency : instanceModel.getDependencies()) {
          BaseInstanceModel dependency = nameToEntity.get(instanceDependency.getIdentity());
          if (dependency == null) {
            dependency = missing.computeIfAbsent(instanceDependency.getIdentity(), s -> new ExpectedModel(s));
            missing.get(instanceDependency.getIdentity())
              .addDefinitionReferenceToType(instanceModel.getIdentity(), instanceDependency.getType());
          }
          if (!entityGraph.containsVertex(dependency)) {
            entityGraph.addVertex(dependency);
          }
          entityGraph.addEdge(entity, dependency);
        }
      }
    }
    
    boolean errored = errorsForCycles(errorListener, entityGraph);
    errored = testAllMissingEntitiesAreExpected(definition, errorListener, missing, entityGraph) || errored;
    errored = errorUnusedExpectedsOnDefinition(definition, errorListener, missing) || errored;
    return errored;
  }

  private boolean checkInstancesTypesInDefinition(final DefinitionModel definition, final Map<String, InstanceModel> nameToEntity,
      final Consumer<ErrorModel> errorListner, AssignabilityUtils assignabilityUtils) {
    boolean errored = false;
    List<InstanceModel> instances = definition.getObjectDefinitions();
    for (InstanceModel instance : instances) {
      for (InstanceDependencyModel instanceDependency : instance.getDependencies()) {
        InstanceModel dependency = nameToEntity.get(instanceDependency.getIdentity());
        if (dependency != null && !assignabilityUtils.isAssignableFrom(dependency, instance)) {
          errored = true;
          errorListner.accept(new ErrorModel(ErrorType.UNMATCHED_TYPES,
              Arrays.asList(instance, dependency), Arrays.asList(instance, dependency)));
        }
      }
    }
    return errored;
  }

  private boolean errorUnusedExpectedsOnDefinition(final DefinitionModel definition, final Consumer<ErrorModel> errorListner,
      final Map<String, ExpectedModel> missing) {
    boolean errored = false;
    for (ExpectedModel expected : definition.getExpectedDefinitions()) {
      if (!missing.keySet().contains(expected.getIdentity())) {
        errorListner.accept(new ErrorModel(ErrorType.UNUSED_EXPECTED, Arrays.asList(expected), Arrays.asList(definition)));
        errored = true;
      }
    }
    return errored;
  }

  private boolean errorsForCycles(final Consumer<ErrorModel> errorListner,
      final DirectedGraph<BaseInstanceModel, DefaultEdge> entityGraph) {
    SzwarcfiterLauerSimpleCycles<BaseInstanceModel, DefaultEdge> cycleFind = new SzwarcfiterLauerSimpleCycles<>();
    boolean errored = false;
    cycleFind.setGraph(entityGraph);
    List<List<BaseInstanceModel>> cycles = cycleFind.findSimpleCycles();
    for (List<BaseInstanceModel> cycle : cycles) {
      errored = true;
      errorListner.accept(new ErrorModel(ErrorType.CYCLE_IN_DEFINITION_SOURCES, cycle, cycle));
    }
    return errored;
  }

  private boolean testAllMissingEntitiesAreExpected(final DefinitionModel definition, final Consumer<ErrorModel> errorListner,
      final Map<String, ExpectedModel> missing, final DirectedGraph<BaseInstanceModel, DefaultEdge> entityGraph) {
    //check computed expected are actually expected
    boolean errored = false;
    List<String> expectedMissing = definition.getExpectedDefinitions().stream().map(em -> em.getIdentity())
        .collect(Collectors.toList());
    for (ExpectedModel expected : missing.values()) {
      if (!expectedMissing.contains(expected.getIdentity())) {
        List<AbstractModel> dependsOnMissing = Stream.concat(
            Stream.of(definition), 
            entityGraph.incomingEdgesOf(expected).stream().map(edge -> entityGraph.getEdgeSource(edge))
              .filter(m -> m.getSourceElement().isPresent()))
            .collect(Collectors.toList());
        errored = true;
        errorListner.accept(new ErrorModel(ErrorType.MISSING_BEAN_DEFINITIONS, Arrays.asList(expected), dependsOnMissing));
      } else {
        definition.addComputedExpected(expected);
      }
    }
    return errored;
  }
}
