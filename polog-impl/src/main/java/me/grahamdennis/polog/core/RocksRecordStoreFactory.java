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

import com.google.common.collect.ImmutableList;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.rocksdb.ColumnFamilyDescriptor;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.ColumnFamilyOptions;
import org.rocksdb.DBOptions;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

public final class RocksRecordStoreFactory {
    private RocksRecordStoreFactory() {}

    public static RocksRecordStore create(Path path) throws RocksDBException {
        ColumnFamilyOptions defaultCfOptions = createDefaultCfOptions();
        ColumnFamilyOptions recordsCfOptions = createRecordsCfOptions();

        List<ColumnFamilyDescriptor> columnFamilyDescriptors = ImmutableList.of(
                new ColumnFamilyDescriptor(RocksDB.DEFAULT_COLUMN_FAMILY, defaultCfOptions),
                new ColumnFamilyDescriptor(RocksRecordStore.RECORDS_COLUMN_FAMILY, recordsCfOptions));

        List<ColumnFamilyHandle> columnFamilyHandles = new ArrayList<>(columnFamilyDescriptors.size());
        DBOptions dbOptions = createDbOptions();

        RocksDB db = RocksDB.open(dbOptions, path.toString(), columnFamilyDescriptors, columnFamilyHandles);

        return RocksRecordStore.builder()
                .defaultCfOptions(defaultCfOptions)
                .defaultCfHandle(columnFamilyHandles.get(0))
                .recordsCfOptions(recordsCfOptions)
                .recordsCfHandle(columnFamilyHandles.get(1))
                .dbOptions(dbOptions)
                .db(db)
                .build();
    }

    private static DBOptions createDbOptions() {
        DBOptions dbOptions = new DBOptions();
        dbOptions.setCreateIfMissing(true);
        dbOptions.setCreateMissingColumnFamilies(true);
        return dbOptions;
    }

    private static ColumnFamilyOptions createDefaultCfOptions() {
        return new ColumnFamilyOptions().optimizeForSmallDb();
    }

    private static ColumnFamilyOptions createRecordsCfOptions() {
        return new ColumnFamilyOptions().optimizeUniversalStyleCompaction();
    }
}
