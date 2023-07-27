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

import io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.exporter.zipkin.ZipkinSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import org.apache.ignite.Ignite;
import org.apache.ignite.configuration.notifications.ConfigurationListener;
import org.apache.ignite.configuration.notifications.ConfigurationNotificationEvent;
import org.apache.ignite.internal.logger.IgniteLogger;
import org.apache.ignite.internal.logger.Loggers;
import org.apache.ignite.internal.tracing.Tracer;
import org.apache.ignite.internal.tracing.TracingComponent;
import org.apache.ignite.internal.tracing.TracingConfigurationParameters;
import org.apache.ignite.internal.tracing.configuration.TracingConfiguration;
import org.apache.ignite.internal.tracing.configuration.TracingView;
import org.jetbrains.annotations.Nullable;

/**
 * Tracing manager.
 */
public class OtelTracingComponent implements TracingComponent, ConfigurationListener<TracingView> {
    /** Logger. */
    private final IgniteLogger log;

    private final Ignite ignite;

    private final OtelBaggageBasedSampler otelBaggageBasedSampler;

    private TracingConfiguration tracingConfiguration;

    private OpenTelemetrySdk openTelemetry;

    private volatile boolean noop = false;

    /**
     * Constructor.
     */
    public OtelTracingComponent(Ignite ignite) {
        this(Loggers.forClass(OtelTracingComponent.class), ignite);
    }

    /**
     * Constructor.
     */
    public OtelTracingComponent(IgniteLogger log, Ignite ignite) {
        this.log = log;
        this.ignite = ignite;
        this.otelBaggageBasedSampler = new OtelBaggageBasedSampler();
    }

    // TODO: 27.07.2023 should be moved to common class
    public static <T> Supplier<T> wrap0(Supplier<T> s) {
        return Context.current().wrapSupplier(s);
    }

    @Override
    public void start() {
        TracingView tracingView = tracingConfiguration.value();
        updateConfig(tracingView);
        otelBaggageBasedSampler.updateConfig(tracingView);

        if (tracingConfiguration.autoConfigure().value()) {
            openTelemetry = AutoConfiguredOpenTelemetrySdk
                    .builder()
                    .addSamplerCustomizer((sampler, configProperties) -> Sampler.parentBasedBuilder(sampler)
                            .setLocalParentSampled(otelBaggageBasedSampler)
                            .setRemoteParentSampled(otelBaggageBasedSampler)
                            .build())
                    .build()
                    .getOpenTelemetrySdk();
        } else {
            Resource resource = Resource.getDefault().merge(Resource.create(
                    Attributes.of(ResourceAttributes.SERVICE_NAME, "ignite", ResourceAttributes.SERVICE_VERSION, "3.0-beta")));

            SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder().setSampler(
                            Sampler.parentBasedBuilder(Sampler.alwaysOn())
                                    .setLocalParentSampled(otelBaggageBasedSampler)
                                    .setRemoteParentSampled(otelBaggageBasedSampler)
                                    .build()).addSpanProcessor(BatchSpanProcessor.builder(ZipkinSpanExporter.builder().build()).build())
                    .setResource(resource).build();

            openTelemetry = OpenTelemetrySdk.builder().setTracerProvider(sdkTracerProvider).setPropagators(ContextPropagators.create(
                    TextMapPropagator.composite(W3CBaggagePropagator.getInstance(), W3CTraceContextPropagator.getInstance()))).build();
        }

        tracingConfiguration.listen(this);
        tracingConfiguration.listen(otelBaggageBasedSampler);
        log.trace("Started");
    }

    @Override
    public void stop() throws Exception {
        openTelemetry.close();
    }

    @Override
    public <T> Supplier<T> wrap(Supplier<T> s) {
        if (noop) {
            return s;
        }
        return Context.current().wrapSupplier(s);
    }

    @Override
    public Tracer getTracer(String scope) {
        if (noop) {
            return Tracer.NOOP;
        }
        return new OtelTracer(openTelemetry.getTracerProvider().get(scope), openTelemetry.getPropagators().getTextMapPropagator(), ignite);
    }

    /**
     * Method to configure {@link TracingComponent} with distributed configuration.
     *
     * @param tracingConfiguration Distributed tracing configuration.
     */
    @Override
    public void configure(TracingConfiguration tracingConfiguration) {
        assert this.tracingConfiguration == null : "Tracing component must be configured only once, on the start of the node";

        this.tracingConfiguration = tracingConfiguration;
    }

    @Override
    public CompletableFuture<?> onUpdate(ConfigurationNotificationEvent<TracingView> ctx) {
        TracingView value = ctx.newValue();
        return CompletableFuture.runAsync(() -> updateConfig(value));
    }

    private void updateConfig(@Nullable TracingView tracingView) {
        noop = tracingView == null || tracingView.ration() == TracingConfigurationParameters.SAMPLING_RATE_NEVER;
    }
}
