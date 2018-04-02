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

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.function.ToIntFunction;
import me.grahamdennis.dag.api.NodeId;

public final class NodeHeightCalculator implements ToIntFunction<NodeId> {
    @Override
    public int applyAsInt(NodeId value) {
        return Integer.numberOfTrailingZeros(getSecureRandom(value).nextInt());
    }

    private SecureRandom getSecureRandom(NodeId value) {
        return new SecureRandom(value.toString().getBytes(StandardCharsets.UTF_8));
    }
}
