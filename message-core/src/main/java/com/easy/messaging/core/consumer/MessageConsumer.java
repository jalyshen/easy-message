package com.easy.messaging.core.consumer;

import com.easy.messaging.core.listener.MessageHandler;

import java.util.Set;

public interface MessageConsumer {

    MessageSubscription subscribe(String subscriberId, Set<String> channels, MessageHandler handler);

    String getId();

    void close();
}
