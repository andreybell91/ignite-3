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

package org.apache.ignite.internal.schema.configuration.index;

import org.apache.ignite.configuration.annotation.InjectedName;
import org.apache.ignite.configuration.annotation.PolymorphicConfig;
import org.apache.ignite.configuration.annotation.PolymorphicId;
import org.apache.ignite.configuration.annotation.Value;
import org.apache.ignite.configuration.validation.Range;

/**
 * SQL index configuration.
 */
@PolymorphicConfig
public class TableIndexConfigurationSchema {
    /** Hash index type. */
    public static final String HASH_INDEX_TYPE = "HASH";

    /** Sorted index type. */
    public static final String SORTED_INDEX_TYPE = "SORTED";

    /** Index type name. */
    @PolymorphicId
    public String type;

    /** Index identifier. */
    @Value(hasDefault = true)
    @Range(min = 1)
    public int id = 1;

    /** Index name. */
    @InjectedName
    public String name;

    /** ID of the table the index is associated with. */
    @Value
    public int tableId;

    /** Has default value flag. */
    @Value(hasDefault = true)
    public boolean uniq = false;
}
