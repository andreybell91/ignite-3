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

package org.apache.ignite.internal.tracing.otel;

import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.apache.ignite.configuration.notifications.ConfigurationListener;
import org.apache.ignite.configuration.notifications.ConfigurationNotificationEvent;
import org.apache.ignite.internal.tracing.configuration.TracingView;
import org.jetbrains.annotations.Nullable;

/**
 * Sample spans if scope id is same as parent scope id.
 */
public class OtelBaggageBasedSampler implements Sampler, ConfigurationListener<TracingView> {
    /** Noop optimization. */
    private volatile boolean noop;

    /**
     * Constructor.
     */
    OtelBaggageBasedSampler() {
        this.noop = true;
    }

    /** {@inheritDoc} */
    @Override
    public SamplingResult shouldSample(Context parentContext, String traceId, String name, SpanKind spanKind, Attributes attributes,
            List<LinkData> parentLinks) {
        if (noop) {
            return SamplingResult.drop();
        }

        String value = Baggage.fromContext(parentContext).getEntryValue("ignite.scope.id");

        if (value == null) {
            return SamplingResult.drop();
        }

        if (Long.valueOf(value).equals(attributes.get(AttributeKey.longKey("ignite.scope.id")))) {
            return SamplingResult.recordAndSample();
        }

        return SamplingResult.drop();
    }

    /** {@inheritDoc} */
    @Override
    public String getDescription() {
        return "OtelSampler";
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<?> onUpdate(ConfigurationNotificationEvent<TracingView> ctx) {
        TracingView value = ctx.newValue();
        return CompletableFuture.runAsync(() -> updateConfig(value));
    }

    /**
     * Update config.
     *
     * @param tracingView tracing view.
     */
    void updateConfig(@Nullable TracingView tracingView) {
        if (tracingView == null || tracingView.scopeSampling() == null) {
            noop = true;
        }
        noop = false;
    }
}
