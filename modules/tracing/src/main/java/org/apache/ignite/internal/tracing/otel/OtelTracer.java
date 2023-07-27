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

import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import java.util.Map;
import java.util.Set;
import org.apache.ignite.Ignite;
import org.apache.ignite.internal.tracing.ContextScope;
import org.apache.ignite.internal.tracing.Span;
import org.apache.ignite.internal.tracing.Tracer;

/**
 * OTel tracer.
 */
public class OtelTracer implements Tracer {

    private final io.opentelemetry.api.trace.Tracer tracer;

    private final TextMapPropagator textMapPropagator;

    private final Ignite ignite;

    private final TextMapSetter<Map<String, String>> setter =
            (carrier, key, value) -> {
                // Insert the context as Header
                carrier.put(key, value);
            };

    private final TextMapGetter<Map<String, String>> getter =
            new TextMapGetter<>() {
                @Override
                public String get(Map<String, String> carrier, String key) {
                    if (carrier.containsKey(key)) {
                        return carrier.get(key);
                    }
                    return null;
                }

                @Override
                public Iterable<String> keys(Map<String, String> carrier) {
                    return carrier == null ? Set.of() : carrier.keySet();
                }
            };


    OtelTracer(io.opentelemetry.api.trace.Tracer tracer, TextMapPropagator textMapPropagator, Ignite ignite) {
        this.tracer = tracer;
        this.textMapPropagator = textMapPropagator;
        this.ignite = ignite;
    }

    @Override
    public Span startSpan(String scope, String name) {
        return new OtelSpan(scope, spanBuilder(scope, name).startSpan());
    }

    @Override
    public ContextScope extract(Map<String, String> headers) {
        return new OtelContextScope(textMapPropagator.extract(Context.current(), headers, getter)
                .makeCurrent());
    }

    @Override
    public void inject(Map<String, String> headers) {
        textMapPropagator.inject(Context.current(), headers, setter);
    }

    private SpanBuilder spanBuilder(String scope, String spanName) {
        return tracer.spanBuilder(spanName)
                .setAttribute("ignite.node.name", ignite.name())
                .setAttribute("ignite.scope.name", scope)
                .setAttribute(SemanticAttributes.THREAD_ID, Thread.currentThread().getId())
                .setAttribute(SemanticAttributes.THREAD_NAME, Thread.currentThread().getName());
    }
}
