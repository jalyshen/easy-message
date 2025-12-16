package com.easy.messaging.core.producer;

import com.easy.messaging.core.message.Message;
import com.easy.messaging.core.message.MessageInterceptor;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Producer of MessageProducer: pick up the MessageProducer based on Destination
 * And at the sometime, merge MessageInterceptors
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
        MessageProducer delegate = delegates.get(destination);

        if (delegate == null) {
            throw new RuntimeException("No message producer configured for destination: " + destination);
        }

        for (MessageInterceptor interceptor : interceptors) {
            try {
                interceptor.preSend(message);
            } catch (Exception e) {
                log.error("Interceptor preSend failed", e);
            }
        }

        try {
            delegate.send(destination, message);
            for (MessageInterceptor interceptor : interceptors) {
                try {
                    interceptor.postSend(message, null);
                } catch (Exception e) {
                    log.error("Interceptor postSend failed", e);
                }
            }
        } catch (Exception e) {
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