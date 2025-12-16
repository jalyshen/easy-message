package com.easy.messaging.autoconfig;

import com.easy.messaging.core.message.MessageInterceptor;
import com.easy.messaging.core.message.tracing.TraceContext;
import com.easy.messaging.core.message.tracing.TraceUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 简单的 Tracing 拦截器配置
 */
@Configuration
public class TracingConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "messaging", name = "tracing-enabled", havingValue = "true", matchIfMissing = true)
    public MessageInterceptor traceInterceptor() {
        return new MessageInterceptor() {
            @Override
            public void preSend(com.easy.messaging.core.message.Message message) {
                // If there is an exists TraceContext, here should inject exists traceContext instance,
                // others, create a new one
                TraceContext ctx = TraceContext.create();
                TraceUtils.inject(message, ctx);
            }

            @Override
            public void preReceive(com.easy.messaging.core.message.Message message) {
                TraceContext ctx = TraceUtils.extractOrCreate(message);
                // Here, should put ctx into MDC or ThreadLocal
            }
        };
    }
}
