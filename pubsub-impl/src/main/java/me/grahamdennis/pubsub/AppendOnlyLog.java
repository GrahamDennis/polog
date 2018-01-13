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

package me.grahamdennis.pubsub;

import java.util.Optional;
import java.util.function.ObjLongConsumer;
import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public interface AppendOnlyLog<E> {
    /**
     * Atomically append an element to the log, and return its index in the log
     * @param element element to append to the end of the log
     * @return the committed index of {@code element} in the log.
     */
    long append(E element);

    /**
     * Returns the element at the specified position in this log.
     * @param index index of the element to return
     * @return element at the specified position in this log
     * @throws IndexOutOfBoundsException if the index is out of range.
     */
    E get(long index);

    long size();

    void subscribeToChanges(ObjLongConsumer<E> subscriber);
    void unsubscribeToChanges(ObjLongConsumer<E> subscriber);

    Optional<E> getOrElseSubscribe(long index, ObjLongConsumer<E> subscriber);
}
