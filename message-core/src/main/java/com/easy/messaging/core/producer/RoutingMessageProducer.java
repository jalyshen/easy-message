package com.easy.messaging.core.producer;

import com.easy.messaging.core.message.Message;
import com.easy.messaging.core.message.MessageInterceptor;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 路由生产者：根据Destination决定使用哪个底层Client发送
 * 同时负责执行拦截器链
 */
public class RoutingMessageProducer implements MessageProducer {

    private static final Logger log = LoggerFactory.getLogger(RoutingMessageProducer.class);

    private final Map<String, MessageProducer> delegates;
    private final List<MessageInterceptor> interceptors = new ArrayList<>();

    public RoutingMessageProducer(Map<String, MessageProducer> delegates) {
        this.delegates = delegates;
    }

    public void addInterceptor(MessageInterceptor interceptor) {
        this.interceptors.add(interceptor);
    }
    public void addInterceptors(List<MessageInterceptor> interceptors) {
        this.interceptors.addAll(interceptors);
    }

    @Override
    public void send(String destination, Message message) {
        // 1. 路由逻辑：这里简单假设 key 就是 resolved destination
        // 如果需要 logical -> physical 映射，应在调用此方法前完成，或注入 Properties 解析
        MessageProducer delegate = delegates.get(destination);

        if (delegate == null) {
            // 尝试查找 default 或者报错
            throw new RuntimeException("No message producer configured for destination: " + destination);
        }

        // 2. 拦截器 PreSend
        for (MessageInterceptor interceptor : interceptors) {
            try {
                interceptor.preSend(message);
            } catch (Exception e) {
                log.error("Interceptor preSend failed", e);
            }
        }

        try {
            // 3. 执行发送
            delegate.send(destination, message);

            // 4. 拦截器 PostSend (Success)
            for (MessageInterceptor interceptor : interceptors) {
                try {
                    interceptor.postSend(message, null);
                } catch (Exception e) {
                    log.error("Interceptor postSend failed", e);
                }
            }
        } catch (Exception e) {
            // 4. 拦截器 PostSend (Error)
            for (MessageInterceptor interceptor : interceptors) {
                try {
                    interceptor.postSend(message, e);
                } catch (Exception ex) {
                    log.error("Interceptor postSend error handle failed", ex);
                }
            }
            throw e;
        }
    }
}