/**
 * Copyright (C) 2009 - 2009 by OpenGamma Inc.
 *
 * Please see distribution for license.
 */
package com.opengamma.engine.depgraph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opengamma.engine.ComputationTarget;
import com.opengamma.engine.ComputationTargetResolver;
import com.opengamma.engine.ComputationTargetType;
import com.opengamma.engine.function.FunctionDefinition;
import com.opengamma.engine.function.FunctionRepository;
import com.opengamma.engine.function.FunctionResolver;
import com.opengamma.engine.function.LiveDataSourcingFunction;
import com.opengamma.engine.livedata.LiveDataAvailabilityProvider;
import com.opengamma.engine.value.ValueRequirement;
import com.opengamma.engine.value.ValueSpecification;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.Pair;

/**
 * 
 *
 * @author kirk
 */
public class DependencyGraphModel {
  private static final Logger s_logger = LoggerFactory.getLogger(DependencyGraphModel.class);
  // Injected Inputs:
  private LiveDataAvailabilityProvider _liveDataAvailabilityProvider;
  private FunctionRepository _functionRepository;
  private ComputationTargetResolver _targetResolver;
  private FunctionResolver _functionResolver;
  // State:
  private final Map<ComputationTarget, DependencyGraph> _graphsByTarget =
    new HashMap<ComputationTarget, DependencyGraph>();
  private final Set<ValueRequirement> _allRequiredLiveData = new HashSet<ValueRequirement>();
    
  /**
   * @return the liveDataAvailabilityProvider
   */
  public LiveDataAvailabilityProvider getLiveDataAvailabilityProvider() {
    return _liveDataAvailabilityProvider;
  }
  /**
   * @param liveDataAvailabilityProvider the liveDataAvailabilityProvider to set
   */
  public void setLiveDataAvailabilityProvider(
      LiveDataAvailabilityProvider liveDataAvailabilityProvider) {
    _liveDataAvailabilityProvider = liveDataAvailabilityProvider;
  }
  /**
   * @return the analyticFunctionRepository
   */
  public FunctionRepository getFunctionRepository() {
    return _functionRepository;
  }
  /**
   * @param functionRepository the analyticFunctionRepository to set
   */
  public void setFunctionRepository(
      FunctionRepository functionRepository) {
    _functionRepository = functionRepository;
  }
  
  /**
   * @return the functionResolver
   */
  public FunctionResolver getFunctionResolver() {
    return _functionResolver;
  }
  /**
   * @param functionReolver the functionResolver to set
   */
  public void setFunctionResolver(FunctionResolver functionResolver) {
    _functionResolver = functionResolver;
  }
  /**
   * @return the targetResolver
   */
  public ComputationTargetResolver getTargetResolver() {
    return _targetResolver;
  }
  /**
   * @param targetResolver the targetResolver to set
   */
  public void setTargetResolver(ComputationTargetResolver targetResolver) {
    _targetResolver = targetResolver;
  }
  
  public Set<ValueRequirement> getAllRequiredLiveData() {
    return Collections.unmodifiableSet(_allRequiredLiveData);
  }
  
  public void addTarget(ComputationTarget target, Set<ValueRequirement> requirements) {
    ArgumentChecker.checkNotNull(target, "Computation Target");
    ArgumentChecker.checkNotNull(requirements, "Value requirements");
    
    for(ValueRequirement requirement : requirements) {
      // TODO kirk 2009-12-30 -- Need to do something with the top-most mappings.
      addTargetRequirement(target, requirement);
    }
  }
  
  protected Pair<DependencyNode, ValueSpecification> addTargetRequirement(
      ComputationTarget target, ValueRequirement requirement) {
    s_logger.info("Adding target requirement for {} on {}", requirement, target);
    Pair<DependencyNode, ValueSpecification> existingNode = resolveRequirement(target, requirement);
    if(existingNode != null) {
      s_logger.debug("Existing Node : {} on {}", requirement, target);
      return existingNode;
    }
    
    DependencyGraph depGraph = getDependencyGraph(target);
    
    if(getLiveDataAvailabilityProvider().isAvailable(requirement)) {
      s_logger.debug("Live Data : {} on {}", requirement, target);
      _allRequiredLiveData.add(requirement);
      LiveDataSourcingFunction function = new LiveDataSourcingFunction(requirement);
      DependencyNode node = new DependencyNode(function, target);
      depGraph.addDependencyNode(node);
      return new Pair<DependencyNode, ValueSpecification>(node, function.getResult());
    }
    
    Pair<FunctionDefinition, ValueSpecification> resolvedFunction = getFunctionResolver().resolveFunction(target, requirement);
    if(resolvedFunction == null) {
      // Couldn't resolve.
      // TODO kirk 2009-12-30 -- Gather up all the errors in some way.
      throw new UnsatisfiableDependencyGraphException("Could not satisfy requirement " + requirement + " for target " + target);
    }
    DependencyNode node = new DependencyNode(resolvedFunction.getFirst(), target);
    depGraph.addDependencyNode(node);
    
    for(ValueRequirement inputRequirement : node.getInputRequirements()) {
      ComputationTarget inputTarget = getTargetResolver().resolve(inputRequirement.getTargetSpecification());
      if(inputTarget == null) {
        throw new UnsatisfiableDependencyGraphException("Unable to resolve target for " + inputRequirement);
      }
      Pair<DependencyNode, ValueSpecification> resolvedInput = addTargetRequirement(inputTarget, inputRequirement);
      node.addInputNode(resolvedInput.getFirst());
      node.addRequirementMapping(inputRequirement, resolvedInput.getSecond());
    }
    
    return new Pair<DependencyNode, ValueSpecification>(node, resolvedFunction.getSecond());
  }
  
  protected DependencyGraph getDependencyGraph(ComputationTarget target) {
    DependencyGraph depGraph = _graphsByTarget.get(target);
    if(depGraph == null) {
      depGraph = new DependencyGraph(target);
      _graphsByTarget.put(target, depGraph);
    }
    return depGraph;
  }
  
  protected Pair<DependencyNode, ValueSpecification> resolveRequirement(ComputationTarget target, ValueRequirement requirement) {
    for(DependencyGraph depGraph : _graphsByTarget.values()) {
      Pair<DependencyNode, ValueSpecification> satisfiedRequirement = depGraph.getNodeProducing(requirement);
      if(satisfiedRequirement != null) {
        return satisfiedRequirement;
      }
    }
    return null;
  }
  
  public Collection<DependencyGraph> getAllDependencyGraphs() {
    return new ArrayList<DependencyGraph>(_graphsByTarget.values());
  }
  
  public Collection<DependencyGraph> getDependencyGraphs(ComputationTargetType targetType) {
    List<DependencyGraph> graphs = new ArrayList<DependencyGraph>();
    for(Map.Entry<ComputationTarget, DependencyGraph> entry : _graphsByTarget.entrySet()) {
      if(entry.getKey().getType() == targetType) {
        graphs.add(entry.getValue());
      }
    }
    return graphs;
  }

}
