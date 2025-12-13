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
    @ConditionalOnProperty(prefix = "easy-messaging", name = "tracing-enabled", havingValue = "true", matchIfMissing = true)
    public MessageInterceptor traceInterceptor() {
        return new MessageInterceptor() {
            @Override
            public void preSend(com.easy.messaging.core.message.Message message) {
                // 如果当前上下文有 Trace，注入进去；否则新建
                // 这里简单演示新建
                TraceContext ctx = TraceContext.create();
                TraceUtils.inject(message, ctx);
            }

            @Override
            public void preReceive(com.easy.messaging.core.message.Message message) {
                TraceContext ctx = TraceUtils.extractOrCreate(message);
                // 实际上这里应该把 ctx 放入 MDC 或者 ThreadLocal
            }
        };
    }
}
