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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedSet;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import me.grahamdennis.immutables.ImmutablesStyle;
import me.grahamdennis.polog.api.Record;
import me.grahamdennis.polog.api.RecordId;
import me.grahamdennis.polog.api.RecordStore;
import org.immutables.value.Value;

@Value.Immutable
@ImmutablesStyle
public abstract class BatchRecordStoreTask {
    public abstract List<Record> records();

    @Value.Derived
    @SuppressWarnings("DesignForExtension")
    protected Map<RecordId, Record> recordsById() {
        return records().stream()
                .collect(ImmutableMap.toImmutableMap(Record::id, record -> record));
    }

    @Value.Derived
    @SuppressWarnings("DesignForExtension")
    protected SortedSet<Record> recordsOrderedById() {
        return ImmutableSortedSet.orderedBy(Comparator.comparing(Record::id, RecordId.naturalOrder()))
                .addAll(records())
                .build();
    }

    @Value.Derived
    @SuppressWarnings("DesignForExtension")
    protected SortedSet<RecordId> dependencies() {
        ImmutableSortedSet.Builder<RecordId> dependenciesBuilder =
                ImmutableSortedSet.orderedBy(RecordId.naturalOrder());

        for (Record record : records()) {
            dependenciesBuilder.addAll(record.parents());
            dependenciesBuilder.addAll(record.additionalDependencies().values());
        }

        return dependenciesBuilder.build();
    }

    // Steps:
    // 1. Verify that all record dependencies are either in the batch, or stored in the database.
    //    a) do this in order of the recordId.
    // 2. Insert the records into the database
    //    a) obtain range of local ids for the batch
    //    b) insert the records into the database in a causal order with increasing local ids
    //    c) insert the recordId -> localId entries in recordId order.
    public final void storeBatch(RecordStore recordStore) throws UnsatisfiedDependencyException, IOException {
        // Step 1: verify that all record dependencies are either in the batch, or stored in the database.
        // FIXME(gdennis): create a way to do this on RecordStore as a single batch operation.
        Map<RecordId, Record> recordsById = recordsById();
        for (RecordId recordId : dependencies()) {
            if (!recordsById.containsKey(recordId) && recordStore.recordExists(recordId)) {
                throw Errors.unsatisfiedDependency(recordId);
            }
        }

        // Step 2: Insert the records into the database.
        // FIXME(gdennis): attempt to store records in causal order.
        for (Record record : recordsOrderedById()) {
            recordStore.putRecord(record);
        }

        // FIXME(gdennis): return something useful.
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder extends ImmutableBatchRecordStoreTask.Builder {}
}
