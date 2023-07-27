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

import static org.apache.ignite.internal.tracing.TracingConfigurationParameters.IGNITE_SCOPE_ATTRIBUTE_NAME;

import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Context;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.apache.ignite.internal.tracing.ContextScope;
import org.apache.ignite.internal.tracing.Span;
import org.apache.ignite.internal.tracing.SpanStatus;

/**
 * OTel span.
 */
public class OtelSpan implements Span {
    private static final EnumMap<SpanStatus, StatusCode> STATUS_CODE_MAP = new EnumMap<>(Map.of(
            SpanStatus.OK, StatusCode.OK,
            SpanStatus.CANCELLED, StatusCode.ERROR,
            SpanStatus.ABORTED, StatusCode.ERROR,
            SpanStatus.UNAVAILABLE, StatusCode.UNSET
    ));

    private final String scope;

    private final io.opentelemetry.api.trace.Span span;

    public OtelSpan(String scope, io.opentelemetry.api.trace.Span span) {
        this.scope = scope;
        this.span = span;
    }

    @Override
    public Span setAttribute(String key, String value) {
        span.setAttribute(key, value);
        return this;
    }

    @Override
    public Span setAttribute(String key, List<String> value) {
        span.setAttribute(AttributeKey.stringArrayKey(key), value);
        return this;
    }

    @Override
    public Span setAttribute(String key, long value) {
        span.setAttribute(key, value);
        return this;
    }

    @Override
    public Span setAttribute(String key, double value) {
        span.setAttribute(key, value);
        return this;
    }

    @Override
    public Span addEvent(String name) {
        span.addEvent(name);
        return this;
    }

    @Override
    public Span setStatus(SpanStatus status) {
        span.setStatus(STATUS_CODE_MAP.get(status));
        return this;
    }

    @Override
    public ContextScope makeCurrent() {
        Baggage baggage = Baggage.builder()
                .put(IGNITE_SCOPE_ATTRIBUTE_NAME, scope)
                .build();
        return new OtelContextScope(Context.current().with(span).with(baggage).makeCurrent());
    }

    @Override
    public void end() {
        span.end();
    }
}
