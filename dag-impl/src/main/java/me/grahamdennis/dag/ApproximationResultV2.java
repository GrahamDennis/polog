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
import com.google.common.graph.ImmutableGraph;
import me.grahamdennis.immutables.ImmutablesStyle;
import org.immutables.value.Value;

@Value.Immutable
@ImmutablesStyle
public interface ApproximationResultV2<N> {
    ImmutableGraph<N> approximation();
    SetMultimap<N, N> sourceNodesByApproximationNode();

    int cost();

    static <N> Builder<N> builder() {
        return new Builder<>();
    }

    final class Builder<N> extends ImmutableApproximationResultV2.Builder<N> {}
}
