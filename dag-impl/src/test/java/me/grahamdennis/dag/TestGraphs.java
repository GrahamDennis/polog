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

import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;

public final class TestGraphs {
    public static MutableGraph<Integer> binaryTree(int depth) {
        MutableGraph<Integer> graph = GraphBuilder.directed()
                .allowsSelfLoops(false)
                .expectedNodeCount((1 << (depth + 1)) - 1)
                .build();

        graph.addNode(1);

        for (int idx = 1; idx < (1 << depth); idx++) {
            graph.putEdge(idx, 2 * idx);
            graph.putEdge(idx, 2 * idx + 1);
        }

        return graph;
    }

    public static MutableGraph<Integer> binaryDiamond(int depth) {
        MutableGraph<Integer> graph = binaryTree(depth);

        for (int idx = (1 << (depth - 1)); idx < (1 << depth); idx++) {
            graph.putEdge(2 * idx, -idx);
            graph.putEdge(2 * idx + 1, -idx);
        }

        for (int idx = 1; idx < (1 << (depth - 1)); idx++) {
            graph.putEdge(-(2 * idx), -idx);
            graph.putEdge(-(2 * idx + 1), -idx);
        }

        return graph;
    }
}
