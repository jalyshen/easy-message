package com.jaly.messaging.core.consumer;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MessageListener {

    /**
     * 逻辑上的 destination 名称。
     * 由 MessagingProperties 做 destination -> 真正 topic/hub 映射。
     */
    String destination();

    /**
     * 是否需要自动 ack（不同 MQ 实现可以选择忽略）。
     */
    boolean autoAck() default true;

    /**
     * 订阅者 Id，为了做到多实例隔离（不同 consumer group / eventhub consumer group）。
     * 如果为空，可以由 scanner 自动生成。
     */
    String subscriberId() default "";
}
