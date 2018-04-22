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

import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.google.common.collect.UnmodifiableIterator;
import com.google.common.graph.Graph;
import java.util.Queue;
import java.util.Set;

public final class BreadthFirstIterator<N> extends UnmodifiableIterator<N> {
    private final Graph<N> graph;
    private final Queue<N> queue = Queues.newArrayDeque();
    private final Set<N> visited = Sets.newHashSet();

    public BreadthFirstIterator(Graph<N> graph, Set<? extends N> roots) {
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
