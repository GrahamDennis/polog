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

import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.google.common.collect.UnmodifiableIterator;
import com.google.common.graph.EndpointPair;
import com.google.common.graph.Graph;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.Graphs;
import com.google.common.graph.ImmutableGraph;
import com.google.common.graph.MutableGraph;
import java.util.Queue;
import java.util.Set;

public final class Approximations {
    private Approximations() {}

    public static <N> ApproximationResult<N> approximate(Graph<N> graph, Set<? extends N> nodes) {
        checkArgument(graph.isDirected());
        checkArgument(!graph.allowsSelfLoops());
        checkArgument(!Graphs.hasCycle(graph));

        // The residual graph is the same as the original graph with all promoted nodes turned into sinks (no
        // outgoing edges)
        MutableGraph<N> residual = withSinks(graph, nodes);
        // The approximation graph only contains nodes in nodes.
        MutableGraph<N> approximation = GraphBuilder.from(graph).expectedNodeCount(nodes.size()).build();

        for (N node : nodes) {
            approximation.addNode(node);
        }
        for (N node : nodes) {
            for (N successor : reachableNodes(residual, graph.successors(node))) {
                if (!nodes.contains(successor)) {
                    continue;
                }
                approximation.putEdge(node, successor);
            }
        }

        return ApproximationResult.<N>builder()
                .approximation(ImmutableGraph.copyOf(approximation))
                .residual(ImmutableGraph.copyOf(residual))
                .build();
    }

    public static <N> Iterable<N> reachableNodes(Graph<N> graph, Set<? extends N> sources) {
        checkArgument(graph.nodes().containsAll(sources));
        return () -> new BreadthFirstIterator<>(graph, sources);
    }

    public static <N> MutableGraph<N> withSinks(Graph<N> graph, Set<? extends N> sinks) {
        checkArgument(graph.isDirected());

        MutableGraph<N> result = GraphBuilder.from(graph).expectedNodeCount(graph.nodes().size()).build();
        for (N node : graph.nodes()) {
            result.addNode(node);
        }
        for (EndpointPair<N> edge : graph.edges()) {
            if (sinks.contains(edge.source())) {
                continue;
            }
            result.putEdge(edge.source(), edge.target());
        }
        return result;
    }

    private static final class BreadthFirstIterator<N> extends UnmodifiableIterator<N> {
        private final Graph<N> graph;
        private final Queue<N> queue = Queues.newArrayDeque();
        private final Set<N> visited = Sets.newHashSet();

        private BreadthFirstIterator(Graph<N> graph, Set<? extends N> roots) {
            this.graph = graph;
            queue.addAll(roots);
            visited.addAll(roots);
        }

        @Override
        public boolean hasNext() {
            return !queue.isEmpty();
        }

        @Override
        public N next() {
            N current = queue.remove();
            for (N successor : graph.successors(current)) {
                if (visited.add(successor)) {
                    queue.add(successor);
                }
            }
            return current;
        }
    }
}
