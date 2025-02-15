/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ignite.internal.schema;

/**
 * Class for extracting a subset of columns from {@code BinaryRow}s.
 */
public interface ColumnsExtractor {
    /**
     * Extracts a subset of columns from a given {@code BinaryRow}, that only contains a key.
     *
     * @param keyOnlyRow Row that only contains a key.
     * @return Subset of columns, packed into a {@code BinaryTuple}.
     */
    BinaryTuple extractColumnsFromKeyOnlyRow(BinaryRow keyOnlyRow);

    /**
     * Extracts a subset of columns from a given {@code BinaryRow}.
     *
     * @param row Row with data (both key and value).
     * @return Subset of columns, packed into a {@code BinaryTuple}.
     */
    BinaryTuple extractColumns(BinaryRow row);
}
