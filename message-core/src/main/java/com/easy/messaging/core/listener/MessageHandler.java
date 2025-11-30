package com.easy.messaging.core.listener;

import com.easy.messaging.core.message.Message;

import java.util.function.Consumer;

public interface MessageHandler extends Consumer<Message> {
}
