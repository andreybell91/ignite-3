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

package org.apache.ignite.internal.placementdriver;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.apache.ignite.internal.utils.RebalanceUtil.STABLE_ASSIGNMENTS_PREFIX;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.apache.ignite.internal.affinity.Assignment;
import org.apache.ignite.internal.logger.IgniteLogger;
import org.apache.ignite.internal.logger.Loggers;
import org.apache.ignite.internal.metastorage.EntryEvent;
import org.apache.ignite.internal.metastorage.MetaStorageManager;
import org.apache.ignite.internal.metastorage.WatchEvent;
import org.apache.ignite.internal.metastorage.WatchListener;
import org.apache.ignite.internal.replicator.ReplicationGroupId;
import org.apache.ignite.internal.replicator.TablePartitionId;
import org.apache.ignite.internal.util.ByteUtils;
import org.apache.ignite.internal.util.CollectionUtils;
import org.apache.ignite.internal.util.Cursor;
import org.apache.ignite.internal.vault.VaultEntry;
import org.apache.ignite.internal.vault.VaultManager;
import org.apache.ignite.lang.ByteArray;

/**
 * The class tracks assignment of all replication groups.
 */
public class AssignmentsTracker {
    /** Ignite logger. */
    private static final IgniteLogger LOG = Loggers.forClass(AssignmentsTracker.class);

    /** Vault manager. */
    private final VaultManager vaultManager;

    /** Meta storage manager. */
    private final MetaStorageManager msManager;

    /** Map replication group id to assignment nodes. */
    private final Map<ReplicationGroupId, Set<Assignment>> groupAssignments;

    /** Assignment Meta storage watch listener. */
    private final AssignmentsListener assignmentsListener;

    /**
     * The constructor.
     *
     * @param vaultManager Vault manager.
     * @param msManager Metastorage manager.
     */
    public AssignmentsTracker(VaultManager vaultManager, MetaStorageManager msManager) {
        this.vaultManager = vaultManager;
        this.msManager = msManager;

        this.groupAssignments = new ConcurrentHashMap<>();
        this.assignmentsListener = new AssignmentsListener();
    }

    /**
     * Restores assignments form Vault and subscribers on further updates.
     */
    public void startTrack() {
        msManager.registerPrefixWatch(ByteArray.fromString(STABLE_ASSIGNMENTS_PREFIX), assignmentsListener);

        try (Cursor<VaultEntry> cursor = vaultManager.range(
                ByteArray.fromString(STABLE_ASSIGNMENTS_PREFIX),
                ByteArray.fromString(incrementLastChar(STABLE_ASSIGNMENTS_PREFIX))
        )) {
            for (VaultEntry entry : cursor) {
                String key = entry.key().toString();

                key = key.replace(STABLE_ASSIGNMENTS_PREFIX, "");

                TablePartitionId grpId = TablePartitionId.fromString(key);

                Set<Assignment> assignments = ByteUtils.fromBytes(entry.value());

                groupAssignments.put(grpId, assignments);
            }
        }

        LOG.info("Assignment cache initialized for placement driver [groupAssignments={}]", groupAssignments);
    }

    private static String incrementLastChar(String str) {
        char lastChar = str.charAt(str.length() - 1);

        return str.substring(0, str.length() - 1) + (char) (lastChar + 1);
    }

    /**
     * Stops the tracker.
     */
    public void stopTrack() {
        msManager.unregisterWatch(assignmentsListener);
    }

    /**
     * Gets assignments.
     *
     * @return Map replication group id to its assignment.
     */
    public Map<ReplicationGroupId, Set<Assignment>> assignments() {
        return groupAssignments;
    }

    /**
     * Meta storage assignments watch.
     */
    private class AssignmentsListener implements WatchListener {
        @Override
        public CompletableFuture<Void> onUpdate(WatchEvent event) {
            assert !event.entryEvents().stream().anyMatch(e -> e.newEntry().empty()) : "New assignments are empty";

            if (LOG.isDebugEnabled()) {
                LOG.debug("Assignment update [revision={}, keys={}]", event.revision(),
                        event.entryEvents().stream()
                                .map(e -> new ByteArray(e.newEntry().key()).toString())
                                .collect(Collectors.joining(",")));
            }

            boolean leaseRenewalRequired = false;

            for (EntryEvent evt : event.entryEvents()) {
                var replicationGrpId = TablePartitionId.fromString(
                        new String(evt.newEntry().key(), StandardCharsets.UTF_8).replace(STABLE_ASSIGNMENTS_PREFIX, ""));

                if (evt.newEntry().tombstone()) {
                    groupAssignments.remove(replicationGrpId);
                } else {
                    Set<Assignment> prevAssignment = groupAssignments.put(replicationGrpId, ByteUtils.fromBytes(evt.newEntry().value()));

                    if (CollectionUtils.nullOrEmpty(prevAssignment)) {
                        leaseRenewalRequired = true;
                    }
                }
            }

            if (leaseRenewalRequired) {
                triggerToRenewLeases();
            }

            return completedFuture(null);
        }

        @Override
        public void onError(Throwable e) {
        }
    }

    /**
     * Triggers to renew leases forcibly. The method wakes up the monitor of {@link LeaseUpdater}.
     */
    private void triggerToRenewLeases() {
        //TODO: IGNITE-18879 Implement lease maintenance.
    }
}
