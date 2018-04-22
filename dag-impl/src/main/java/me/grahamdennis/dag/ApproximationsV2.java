/*
 * (c) Copyright 2018 Graham Dennis. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.grahamdennis.dag;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Queues;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import com.google.common.graph.Graph;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.Graphs;
import com.google.common.graph.ImmutableGraph;
import com.google.common.graph.MutableGraph;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

public final class ApproximationsV2 {
    private ApproximationsV2() {}

    public static <N> ApproximationResultV2<N> approximate(Graph<N> graph, int maximumBlockSize) {
        checkArgument(graph.isDirected());
        checkArgument(!graph.allowsSelfLoops());
        checkArgument(!Graphs.hasCycle(graph));

        Set<N> sources = graph.nodes().stream()
                .filter(node -> graph.predecessors(node).isEmpty())
                .collect(Collectors.toSet());

        List<N> topologicalOrder = ImmutableList.copyOf(new BreadthFirstIterator<>(graph, sources));

        return approximate(graph, maximumBlockSize, topologicalOrder);
    }

    /**
     * Kernighan's Algorithm [Kernighan:1971].
     */
    public static <N> ApproximationResultV2<N> approximate(Graph<N> graph, int maximumBlockSize,
            List<N> topologicalOrder) {
        checkArgument(isTopologicalOrder(topologicalOrder, graph));
        ToIntFunction<N> nodeIndexFunction = createNodeIndexFunction(topologicalOrder);

        List<PartialApproximationV2Result<N>> partialResults = runKernighanDynamicProgramming(
                graph,
                maximumBlockSize,
                topologicalOrder,
                nodeIndexFunction);

        int currentBreakIdx = topologicalOrder.size();
        SetMultimap<N, N> sourceNodesByApproximateNode = MultimapBuilder.hashKeys().hashSetValues().build();
        Map<N, N> approximateNodeBySourceNode = Maps.newHashMap();
        while (partialResults.get(currentBreakIdx).previousBreakIdx().isPresent()) {
            int previousBreakIdx = partialResults.get(currentBreakIdx).previousBreakIdx().getAsInt();
            N previousBreakNode = topologicalOrder.get(previousBreakIdx);
            for (N node : topologicalOrder.subList(previousBreakIdx, currentBreakIdx)) {
                sourceNodesByApproximateNode.put(previousBreakNode, node);
                approximateNodeBySourceNode.put(node, previousBreakNode);
            }

            currentBreakIdx = previousBreakIdx;
        }

        MutableGraph<N> approximatedGraph = GraphBuilder.directed().build();
        for (N fromNode : graph.nodes()) {
            for (N toNode : graph.successors(fromNode)) {
                N approximatedFrom = approximateNodeBySourceNode.get(fromNode);
                N approximatedTo = approximateNodeBySourceNode.get(toNode);
                if (!approximatedFrom.equals(approximatedTo)) {
                    approximatedGraph.putEdge(approximatedFrom, approximatedTo);
                }
            }
        }

        PartialApproximationV2Result<N> lastPartialResult = Iterables.getLast(partialResults);

        return ApproximationResultV2.<N>builder()
                .cost(lastPartialResult.cost())
                .sourceNodesByApproximationNode(sourceNodesByApproximateNode)
                .approximation(ImmutableGraph.copyOf(approximatedGraph))
                .build();
    }

    private static <N> List<PartialApproximationV2Result<N>> runKernighanDynamicProgramming(Graph<N> graph,
            int maximumBlockSize, List<N> topologicalOrder, ToIntFunction<N> nodeIndexFunction) {
        List<PartialApproximationV2Result<N>> partialResults =
                Lists.newArrayListWithCapacity(topologicalOrder.size() + 1);
        partialResults.add(PartialApproximationV2Result.<N>builder().build());

        for (int idx = 1; idx <= topologicalOrder.size(); idx++) {
            partialResults.add(null);

            int currentBreakIdx = idx;
            int previousBreakIdx = currentBreakIdx - 1;
            while (previousBreakIdx >= 0
                    && cost(previousBreakIdx, currentBreakIdx, topologicalOrder) <= maximumBlockSize) {
                Set<DirectedEdge<N>> brokenEdges = topologicalOrder.subList(previousBreakIdx, currentBreakIdx).stream()
                        .flatMap(node -> graph.successors(node).stream()
                                .filter(successor -> nodeIndexFunction.applyAsInt(successor) >= currentBreakIdx)
                                .map(successor -> DirectedEdge.of(node, successor)))
                        .collect(Collectors.toSet());

                PartialApproximationV2Result<N> previousPartialResult = partialResults.get(previousBreakIdx);

                PartialApproximationV2Result<N> partialResult = PartialApproximationV2Result.<N>builder()
                        .previousBreakIdx(previousBreakIdx)
                        .addAllCutEdges(brokenEdges)
                        .addAllCutEdges(previousPartialResult.cutEdges())
                        .build();

                PartialApproximationV2Result<N> currentBest = partialResults.get(idx);

                if (currentBest == null || partialResult.cost() < currentBest.cost()) {
                    partialResults.set(idx, partialResult);
                }

                previousBreakIdx--;
            }
        }
        return partialResults;
    }

    private static <N> int cost(int previousBreakIdx, int currentBreakIdx, List<N> topologicalOrder) {
        return currentBreakIdx - previousBreakIdx;
    }

    private static <N> boolean isTopologicalOrder(List<N> topologicalOrder, Graph<N> graph) {
        ToIntFunction<N> nodeIndex = createNodeIndexFunction(topologicalOrder);
        return topologicalOrder.stream()
                .allMatch(node -> graph.successors(node).stream()
                        .allMatch(successor -> nodeIndex.applyAsInt(node) < nodeIndex.applyAsInt(successor)));
    }

    private static <N> ToIntFunction<N> createNodeIndexFunction(List<N> list) {
        ImmutableMap.Builder<N, Integer> builder = ImmutableMap.builder();
        int idx = 0;
        for (N node : list) {
            builder.put(node, idx++);
        }

        return builder.build()::get;
    }

    public static <N> ImmutableGraph<Set<N>> causalPowersetGraph(Graph<N> sourceGraph) {
        MutableGraph<Set<N>> powersetGraph = GraphBuilder.directed().build();

        Queue<Set<N>> queue = Queues.newArrayDeque();
        Set<Set<N>> visited = Sets.newHashSet();
        Set<N> root = ImmutableSet.copyOf(sourceGraph.nodes());
        queue.add(root);
        visited.add(root);

        while (!queue.isEmpty()) {
            Set<N> current = queue.remove();

            Set<N> nodesToRemove = current.stream()
                    .filter(node -> sourceGraph.predecessors(node).stream().noneMatch(current::contains))
                    .collect(Collectors.toSet());

            for (N nodeToRemove : nodesToRemove) {
                Set<N> successor = ImmutableSet.copyOf(Sets.difference(current, ImmutableSet.of(nodeToRemove)));
                powersetGraph.putEdge(current, successor);
                if (visited.add(successor)) {
                    queue.add(successor);
                }
            }
        }

        return ImmutableGraph.copyOf(powersetGraph);
    }
}
