package com.jaly.messaging.core.consumer;

import com.jaly.messaging.core.message.Message;

import java.util.function.Consumer;

public interface MessageHandler extends Consumer<Message> {
}
