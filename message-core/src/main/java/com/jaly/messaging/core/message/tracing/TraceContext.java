package com.jaly.messaging.core.message.tracing;

import java.util.Objects;
import java.util.UUID;

public class TraceContext {

    private final String traceId;

    private final String spanId;

    private TraceContext(String traceId, String spanId) {
        this.traceId = traceId;
        this.spanId = spanId;
    }

    public static TraceContext of(String traceId, String spanId) {
        return new TraceContext(
            traceId == null ? generateTraceId() : traceId,
            spanId  == null ? generateSpanId()  : spanId
        );
    }

    public static TraceContext create() {
        return new TraceContext(generateTraceId(), generateSpanId());
    }

    public static TraceContext childOf(TraceContext parent) {
        if (parent == null) {
            return create();
        }

        return new TraceContext(parent.getTraceId(), parent.getSpanId());
    }

    public static TraceContext continueFrom(String traceId) {
        return new TraceContext(traceId, generateSpanId());
    }

    public String getTraceId() {
        return this.traceId;
    }

    public String getSpanId() {
        return this.spanId;
    }


    @Override
    public String toString() {
        return "TraceContext{" +
                "traceId='" + traceId + '\'' +
                ", spanId='" + spanId + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TraceContext)) return false;
        TraceContext that = (TraceContext) o;
        return Objects.equals(traceId, that.traceId) &&
                Objects.equals(spanId, that.spanId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(traceId, spanId);
    }

    private static String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private static String generateSpanId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

}
