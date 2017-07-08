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
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.jgrapht.DirectedGraph;
import org.jgrapht.alg.cycle.SzwarcfiterLauerSimpleCycles;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import com.salesforce.apt.graph.model.DefinitionModel;
import com.salesforce.apt.graph.model.errors.ErrorModel;
import com.salesforce.apt.graph.model.errors.ErrorType;

public class DefinitionGraphInpector {

  public Set<DefinitionModel> inspectDefinitionGraph(Collection<DefinitionModel> definitions,
      Consumer<ErrorModel> errorListener) {
    DirectedGraph<DefinitionModel, DefaultEdge> graph = buildDefinitionGraph(definitions);
    errorForCycles(graph, errorListener);
    //TODO return all DefinitionModel needing re-evaluation
    return graph.vertexSet().stream().filter(dm -> graph.inDegreeOf(dm) == 0).collect(Collectors.toSet());
  }
  
  private void errorForCycles(DirectedGraph<DefinitionModel, DefaultEdge> definitionGraph,
      Consumer<ErrorModel> errorListener) {
    SzwarcfiterLauerSimpleCycles<DefinitionModel, DefaultEdge> cycleFind = new SzwarcfiterLauerSimpleCycles<>();
    cycleFind.setGraph(definitionGraph);
    for (List<DefinitionModel> list : cycleFind.findSimpleCycles()) {
      errorListener.accept(new ErrorModel(ErrorType.CYCLE_IN_DEFINITION_SOURCES, list, list));
    }
  }
  
  private DirectedGraph<DefinitionModel, DefaultEdge> buildDefinitionGraph(Collection<DefinitionModel> definitions) {
    DirectedGraph<DefinitionModel, DefaultEdge> definitionGraph = new DefaultDirectedGraph<>(DefaultEdge.class);
    buildDefinitionGraph(definitions, definitionGraph);
    return definitionGraph;
  }

  private void buildDefinitionGraph(Collection<DefinitionModel> definitions,
      DirectedGraph<DefinitionModel, DefaultEdge> definitionGraph) {
    for (DefinitionModel definition : definitions) {
      if (!definitionGraph.containsVertex(definition)) {
        definitionGraph.addVertex(definition);
        buildDefinitionGraph(definition.getDependencies(), definitionGraph);
        for (DefinitionModel dependency : definition.getDependencies()) {
          definitionGraph.addEdge(definition, dependency);
        }
      }
    }
  }
  
}
