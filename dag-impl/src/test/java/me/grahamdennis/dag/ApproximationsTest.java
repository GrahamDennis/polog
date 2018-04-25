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

import com.google.common.collect.SetMultimap;
import com.google.common.graph.Graph;
import com.google.common.graph.ImmutableGraph;
import com.google.common.graph.MutableGraph;
import com.palantir.common.streams.KeyedStream;
import guru.nidi.graphviz.attribute.Color;
import guru.nidi.graphviz.attribute.Shape;
import guru.nidi.graphviz.engine.Engine;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.Factory;
import guru.nidi.graphviz.model.Link;
import guru.nidi.graphviz.model.MutableNode;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Test;

public class ApproximationsTest {
    @Test
    public void canApproximateBinaryTree() throws IOException {
        MutableGraph<Integer> binaryTree = TestGraphs.binaryTree(10);

        Set<Integer> higherNodes = binaryTree.nodes()
                .stream()
                .filter(ignored -> new Random().nextBoolean())
                .collect(Collectors.toSet());

        ApproximationResult<Integer> approximation = Approximations.approximate(binaryTree, higherNodes);

        writeToFile(toGraphViz(binaryTree, approximation), new File("build/binary-tree.svg"));
    }

    @Test
    public void canApproximateBinaryDiamond() throws IOException {
        MutableGraph<Integer> binaryDiamond = TestGraphs.binaryDiamond(5);

        Set<Integer> higherNodes = binaryDiamond.nodes()
                .stream()
                .filter(ignored -> new Random().nextBoolean())
                .collect(Collectors.toSet());

        ApproximationResult<Integer> approximation = Approximations.approximate(binaryDiamond, higherNodes);

//        writeToFile(toGraphViz(binaryDiamond), new File("build/binary-diamond.svg"));
        writeToFile(toGraphViz(binaryDiamond, approximation), new File("build/binary-diamond.svg"));
        writeToFile(toGraphViz(approximation.residual()), new File("build/binary-diamond-residual.svg"));
    }

    @Test
    public void canApproximateBinaryDiamondV2() throws IOException {
        MutableGraph<Integer> binaryDiamond = TestGraphs.binaryDiamond(3);
        int maximumBlockSize = binaryDiamond.nodes().size() / 2;

//        writeToFile(
//                toGraphViz(ApproximationsV2.causalPowersetGraph(binaryDiamond)),
//                new File("build/binary-diamond-causal-powerset.svg"));

        ApproximationResultV2<Integer> approximation =
                ApproximationsV2.approximate(binaryDiamond, maximumBlockSize);

        writeToFile(toGraphViz(binaryDiamond, approximation), new File("build/binary-diamond-v2.svg"));

//        ApproximationResultV2<Integer> approximation2 =
//                ApproximationsV2.approximate(binaryDiamond, 5,
//                        ImmutableList.of(1, 2, 4, 5, -2, 3, 6, 7, -3, -1));
//
//        writeToFile(toGraphViz(binaryDiamond, approximation2), new File("build/binary-diamond2-v2.svg"));
//
        ApproximationResultV2<Integer> bestApproximation = ApproximationsV2.approximateFull(binaryDiamond,
                maximumBlockSize);

        writeToFile(toGraphViz(binaryDiamond, bestApproximation), new File("build/binary-diamond-v2-best.svg"));
    }

    private void writeToFile(Graphviz graphviz, File file) throws IOException {
        graphviz.engine(Engine.DOT)
                .render(Format.SVG)
                .toFile(file);
    }

    private <N> Graphviz toGraphViz(Graph<N> sourceGraph) {
        guru.nidi.graphviz.model.Graph graph = Factory.graph().directed();
        for (N node : sourceGraph.nodes()) {
            graph = graph.with(node(node)
                    .addLink(sourceGraph.successors(node)
                            .stream()
                            .map(this::node)
                            .toArray(MutableNode[]::new)));
        }
        return Graphviz.fromGraph(graph);
    }

    private <N> Graphviz toGraphViz(Graph<N> sourceGraph, ApproximationResult<N> approximationResult) {
        guru.nidi.graphviz.model.MutableGraph graph = Factory.mutGraph().setDirected(true);
        ImmutableGraph<N> approximation = approximationResult.approximation();
        Map<N, MutableNode> nodes = KeyedStream.of(sourceGraph.nodes())
                .map(this::node)
                .map((sourceNode, vizNode) -> !approximation.nodes().contains(sourceNode)
                        ? vizNode.add(Shape.CIRCLE)
                        : vizNode.add(Shape.DOUBLE_CIRCLE, Color.RED))
                .collectToMap();
        for (N node : sourceGraph.nodes()) {
            MutableNode vizSource = nodes.get(node);
            graph.add(vizSource);
            for (N successor : sourceGraph.successors(node)) {
                vizSource.addLink(nodes.get(successor));
            }
            if (approximation.nodes().contains(node)) {
                for (N successor : approximation.successors(node)) {
                    vizSource.addLink(
                            Link.to(nodes.get(successor))
                                    .with(Color.RED));
                }
            }
        }
        return Graphviz.fromGraph(graph);
    }

    private <N> Graphviz toGraphViz(Graph<N> sourceGraph, ApproximationResultV2<N> approximation) {
        guru.nidi.graphviz.model.MutableGraph graph = Factory.mutGraph().setDirected(true);
        SetMultimap<N, N> approximationNodesBySourceNodes = approximation.sourceNodesByApproximationNode();

        Map<N, MutableNode> outerNodes = KeyedStream.of(sourceGraph.nodes())
                .map(this::node)
                .collectToMap();

        for (MutableNode vizNode : outerNodes.values()) {
            graph.add(vizNode);
        }

        Map<N, guru.nidi.graphviz.model.MutableGraph> subgraphs = KeyedStream.of(approximation.approximation().nodes())
                .map(this::subgraph)
                .collectToMap();

        for (N approximationNode : approximationNodesBySourceNodes.keySet()) {
            guru.nidi.graphviz.model.MutableGraph subgraph = subgraphs.get(approximationNode);

            Set<N> blockNodes = approximationNodesBySourceNodes.get(approximationNode);
            Map<N, MutableNode> nodes = KeyedStream.of(blockNodes)
                    .map(this::node)
                    .collectToMap();
            for (MutableNode vizNode : nodes.values()) {
                subgraph.add(vizNode);
            }
            for (N sourceNode : blockNodes) {
                MutableNode vizSourceNode = nodes.get(sourceNode);
                for (N successorNode : sourceGraph.successors(sourceNode)) {
                    if (blockNodes.contains(successorNode)) {
                        vizSourceNode.addLink(nodes.get(successorNode));
                    } else {
                        outerNodes.get(sourceNode).addLink(outerNodes.get(successorNode));
                    }
                }
            }
            graph.graphs().add(subgraph);
        }

        return Graphviz.fromGraph(graph);
    }

    private <N> MutableNode node(N node) {
        return Factory.mutNode(node.toString());
    }

    private <N> guru.nidi.graphviz.model.MutableGraph subgraph(N approximateNode) {
        return Factory.mutGraph(approximateNode.toString())
                .setDirected(true)
                .setCluster(true)
                .generalAttrs().add(Color.BLUE);
    }
}
