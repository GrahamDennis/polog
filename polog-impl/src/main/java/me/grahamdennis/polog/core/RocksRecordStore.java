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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import me.grahamdennis.immutables.ImmutablesStyle;
import me.grahamdennis.polog.api.Record;
import me.grahamdennis.polog.api.RecordId;
import me.grahamdennis.polog.api.RecordStore;
import org.immutables.value.Value;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.ColumnFamilyOptions;
import org.rocksdb.DBOptions;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

@Value.Immutable
@ImmutablesStyle
public abstract class RocksRecordStore implements RecordStore, AutoCloseable {
    public static final byte[] RECORDS_COLUMN_FAMILY = "records".getBytes(StandardCharsets.UTF_8);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    protected abstract DBOptions dbOptions();
    protected abstract RocksDB db();

    protected abstract ColumnFamilyOptions defaultCfOptions();
    protected abstract ColumnFamilyHandle defaultCfHandle();

    protected abstract ColumnFamilyOptions recordsCfOptions();
    protected abstract ColumnFamilyHandle recordsCfHandle();

    @Value.Derived
    @SuppressWarnings("DesignForExtension")
    protected List<ColumnFamilyHandle> columnFamilyHandles() {
        return ImmutableList.of(defaultCfHandle(), recordsCfHandle());
    }

    @Value.Derived
    @SuppressWarnings("DesignForExtension")
    protected List<ColumnFamilyOptions> columnFamilyOptions() {
        return ImmutableList.of(defaultCfOptions(), recordsCfOptions());
    }

    @Override
    public final void putRecord(Record record) throws IOException {
        try {
            db().put(recordsCfHandle(), toKeyBytes(record.id()), toValueBytes(record));
        } catch (RocksDBException e) {
            throw new IOException(e);
        }
    }

    @Override
    public final Optional<Record> getRecord(RecordId recordId) throws IOException {
        try {
            byte[] bytes = db().get(recordsCfHandle(), toKeyBytes(recordId));
            if (bytes == null) {
                return Optional.empty();
            } else {
                return Optional.of(fromValueBytes(bytes));
            }
        } catch (RocksDBException e) {
            throw new IOException(e);
        }
    }

    @Override
    public final boolean recordExists(RecordId recordId) throws IOException {
        // FIXME(gdennis): make this more performant, and add a batch version.
        return getRecord(recordId).isPresent();
    }

    @Override
    public final void close() throws Exception {
        for (ColumnFamilyHandle columnFamilyHandle : columnFamilyHandles()) {
            columnFamilyHandle.close();
        }

        db().close();
        dbOptions().close();

        for (ColumnFamilyOptions columnFamilyOptions : columnFamilyOptions()) {
            columnFamilyOptions.close();
        }
    }

    private byte[] toKeyBytes(RecordId recordId) {
        return recordId.id().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] toValueBytes(Record record) throws JsonProcessingException {
        return objectMapper.writeValueAsBytes(record);
    }

    private Record fromValueBytes(byte[] valueBytes) throws IOException {
        return objectMapper.readValue(valueBytes, Record.class);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder extends ImmutableRocksRecordStore.Builder {}
}
