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

package me.grahamdennis.pubsub.appendonlylog;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.ObjLongConsumer;
import javax.annotation.Nullable;

public final class InMemoryAppendOnlyLog<E> implements AppendOnlyLog<E> {

    private final List<E> delegate;
    private final Object mutex;
    private final Set<ObjLongConsumer<E>> subscribers;

    public static <E> AppendOnlyLog<E> create() {
        return new InMemoryAppendOnlyLog<>(Lists.newArrayList(), null, Sets.newConcurrentHashSet());
    }

    public InMemoryAppendOnlyLog(List<E> delegate, @Nullable Object mutex,
            Set<ObjLongConsumer<E>> subscribers) {
        this.delegate = delegate;
        this.mutex = mutex == null ? this : mutex;
        this.subscribers = subscribers;
    }

    @Override
    public long append(E element) {
        synchronized (mutex) {
            delegate.add(element);
            int elementIndex = delegate.size() - 1;
            for (ObjLongConsumer<E> subscriber : subscribers) {
                subscriber.accept(element, elementIndex);
            }
            return elementIndex;
        }
    }

    @Override
    public E get(long index) {
        synchronized (mutex) {
            return delegate.get((int) index);
        }
    }

    @Override
    public long size() {
        synchronized (mutex) {
            return delegate.size();
        }
    }

    @Override
    public void subscribeToChanges(ObjLongConsumer<E> subscriber) {
        synchronized (mutex) {
            subscribers.add(subscriber);
        }
    }

    @Override
    public void unsubscribeToChanges(ObjLongConsumer<E> subscriber) {
        synchronized (mutex) {
            subscribers.remove(subscriber);
        }
    }

    @Override
    public Optional<E> getOrElseSubscribe(long index, ObjLongConsumer<E> subscriber) {
        synchronized (mutex) {
            if (index < delegate.size()) {
                return Optional.of(delegate.get((int) index));
            } else {
                subscribers.add(subscriber);
                return Optional.empty();
            }
        }
    }
}
