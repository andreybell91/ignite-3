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

package org.apache.ignite.internal.table.distributed.schema;

import java.util.concurrent.CompletableFuture;
import java.util.function.LongSupplier;
import org.apache.ignite.internal.catalog.CatalogService;
import org.apache.ignite.internal.hlc.HybridTimestamp;
import org.apache.ignite.internal.metastorage.server.time.ClusterTime;

/**
 * A default implementation of {@link SchemaSyncService}.
 */
public class SchemaSyncServiceImpl implements SchemaSyncService {
    private final ClusterTime clusterTime;

    private final CatalogService catalogService;

    private final LongSupplier delayDurationMs;

    /**
     * Constructor.
     */
    public SchemaSyncServiceImpl(ClusterTime clusterTime, CatalogService catalogService, LongSupplier delayDurationMs) {
        this.clusterTime = clusterTime;
        this.catalogService = catalogService;
        this.delayDurationMs = delayDurationMs;
    }

    @Override
    public CompletableFuture<Void> waitForMetadataCompleteness(HybridTimestamp ts) {
        return clusterTime.waitFor(ts.subtractPhysicalTime(delayDurationMs.getAsLong()));
    }

    @Override
    public boolean isMetadataAvailableFor(int catalogVersion) {
        return catalogVersion <= catalogService.latestCatalogVersion();
    }
}
