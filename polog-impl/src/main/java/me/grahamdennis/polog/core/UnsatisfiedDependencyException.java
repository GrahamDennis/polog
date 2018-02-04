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

import me.grahamdennis.polog.api.RecordId;

public final class UnsatisfiedDependencyException extends Exception {
    private final RecordId recordId;

    public UnsatisfiedDependencyException(String message, RecordId recordId) {
        super(message);
        this.recordId = recordId;
    }

    public RecordId getRecordId() {
        return recordId;
    }
}
