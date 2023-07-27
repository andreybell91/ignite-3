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

package org.apache.ignite.internal.tracing;

import java.util.List;

/**
 * Span.
 */
public interface Span {
    Span NOOP = new NoopSpan();

    Span setAttribute(String key, String value);

    Span setAttribute(String key, List<String> value);

    Span setAttribute(String key, long value);

    Span setAttribute(String key, double value);

    Span addEvent(String name);

    Span setStatus(SpanStatus status);

    ContextScope makeCurrent();

    void end();

    /**
     * Noop span.
     */
    class NoopSpan implements Span {
        @Override
        public Span setAttribute(String key, String value) {
            return this;
        }

        @Override
        public Span setAttribute(String key, List<String> value) {
            return this;
        }

        @Override
        public Span setAttribute(String key, long value) {
            return this;
        }

        @Override
        public Span setAttribute(String key, double value) {
            return this;
        }

        @Override
        public Span addEvent(String name) {
            return this;
        }

        @Override
        public Span setStatus(SpanStatus status) {
            return this;
        }

        @Override
        public ContextScope makeCurrent() {
            return ContextScope.NOOP;
        }

        @Override
        public void end() {
            // noop
        }
    }
}
