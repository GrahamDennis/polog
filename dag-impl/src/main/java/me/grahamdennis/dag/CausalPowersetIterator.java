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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.google.common.collect.UnmodifiableIterator;
import com.google.common.graph.Graph;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

public final class CausalPowersetIterator<N> extends UnmodifiableIterator<Set<N>> {
    private final Graph<N> graph;
    private final Queue<Set<N>> queue = Queues.newArrayDeque();
    private final Set<Set<N>> visited = Sets.newHashSet();

    public CausalPowersetIterator(Graph<N> graph) {
        this.graph = graph;
        Set<N> root = ImmutableSet.copyOf(graph.nodes());
        queue.add(root);
        visited.add(root);
    }

    @Override
    public boolean hasNext() {
        return !queue.isEmpty();
    }

    @Override
    public Set<N> next() {
        Set<N> current = queue.remove();
        Set<N> nodesToRemove = current.stream()
                .filter(node -> current.containsAll(graph.predecessors(node)))
                .collect(Collectors.toSet());

        for (N nodeToRemove : nodesToRemove) {
            Set<N> successor = ImmutableSet.copyOf(Sets.difference(current, ImmutableSet.of(nodeToRemove)));
            if (visited.add(successor)) {
                queue.add(successor);
            }
        }

        return current;
    }
}
