package com.jaly.messaging.core.message.tracing;

import com.jaly.messaging.core.message.Message;

public final class TraceUtils {

    public static final String TRACE_ID = "traceId";
    public static final String SPAN_ID = "spanId";

    private TraceUtils() {}

    public static void inject(Message message, TraceContext ctx) {
        message.setHeader(TRACE_ID, ctx.getTraceId());
        message.setHeader(SPAN_ID, ctx.getSpanId());
    }

    public static TraceContext extractOrCreate(Message message) {
        String traceId = message.getHeader(TRACE_ID).orElse(null);

        if (traceId == null) {
            return TraceContext.create();
        }

        return TraceContext.continueFrom(traceId);
    }
}
