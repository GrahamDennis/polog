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

import java.util.Optional;
import java.util.Set;
import me.grahamdennis.immutables.ImmutablesStyle;
import org.immutables.value.Value;

@Value.Immutable
@ImmutablesStyle
public interface PartialApproximationV2Result<K, N> {
    Set<DirectedEdge<N>> cutEdges();
    Optional<K> previousBreakKey();

    default int cost() {
        return cutEdges().size();
    }

    static <K, N> Builder<K, N> builder() {
        return new Builder<>();
    }

    final class Builder<K, N> extends ImmutablePartialApproximationV2Result.Builder<K, N> {}
}
