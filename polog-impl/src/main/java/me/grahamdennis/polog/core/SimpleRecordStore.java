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

package me.grahamdennis.polog.core;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.MultimapBuilder;
import java.util.Map;
import java.util.Optional;
import me.grahamdennis.polog.api.Record;
import me.grahamdennis.polog.api.RecordId;
import me.grahamdennis.polog.api.RecordKey;
import me.grahamdennis.polog.api.RecordStore;

public final class SimpleRecordStore implements RecordStore {
    private final Map<RecordId, Record> recordMap;
    private final ListMultimap<RecordKey, RecordId> history;

    public static SimpleRecordStore create() {
        return new SimpleRecordStore(Maps.newHashMap(), MultimapBuilder.hashKeys().linkedListValues().build());
    }

    public SimpleRecordStore(Map<RecordId, Record> recordMap,
            ListMultimap<RecordKey, RecordId> history) {
        this.recordMap = recordMap;
        this.history = history;
    }

    @Override
    public void putRecord(Record record) {
        recordMap.put(record.id(), record);
    }

    @Override
    public Optional<Record> getRecord(RecordId recordId) {
        return Optional.ofNullable(recordMap.get(recordId));
    }

    @Override
    public boolean recordExists(RecordId recordId) {
        return recordMap.containsKey(recordId);
    }
}
