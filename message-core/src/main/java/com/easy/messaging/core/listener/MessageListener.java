package com.easy.messaging.core.listener;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MessageListener {

    /**
     * Business destination name
     * Here is the properties sample:
     * <p>
     * defaults:
     *     destinations:
     *       test-logic-name: "test-hub"
     *  the "test-logic-name" is the "destination"
     * </p>
     *
     * And here the sample of usage:
     * <p>
     *  @MessageListener(destination = "test-logic-name", subscriberId = "demo-sub-1")
     *  public void handleMessage(Message message) {
     *     //Here is your codes
     *  }
     *  </p>
     */
    String destination();

    /**
     * auto ack (Could ignore it based on your concrete MQ broker）
     */
    boolean autoAck() default true;

    /**
     * the subscriber Id.
     * To achieve isolation across multiple instances by using different consumer groups / Event Hub consumer groups.
     * scanner will generate one if not set value
     */
    String subscriberId() default "";
}
