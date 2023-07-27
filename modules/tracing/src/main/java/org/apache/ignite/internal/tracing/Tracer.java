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

import java.util.Map;

/**
 * Tracer.
 */
public interface Tracer {

    Tracer NOOP = new NoopTracer();

    /**
     * Create new span. Inherit current context as a child.
     *
     * @param scope span scope.
     * @param name span name.
     * @return tracer.
     */
    Span startSpan(String scope, String name);

    ContextScope extract(Map<String, String> headers);

    void inject(Map<String, String> headers);

    /**
     * NOOP tracer.
     */
    class NoopTracer implements Tracer {
        @Override
        public Span startSpan(String scope, String name) {
            return Span.NOOP;
        }

        @Override
        public ContextScope extract(Map<String, String> headers) {
            return ContextScope.NOOP;
        }

        @Override
        public void inject(Map<String, String> headers) {
            // noop
        }
    }
}
