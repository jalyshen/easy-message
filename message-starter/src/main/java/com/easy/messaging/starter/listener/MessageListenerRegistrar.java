package com.easy.messaging.starter.listener;

import com.easy.messaging.autoconfig.MessagingProperties;
import com.easy.messaging.core.message.Message;
import com.easy.messaging.core.consumer.MessageConsumer;
import com.easy.messaging.core.listener.MessageHandler;
import com.easy.messaging.core.listener.MessageListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;

public class MessageListenerRegistrar implements ApplicationContextAware, SmartInitializingSingleton {
    private static final Logger log = LoggerFactory.getLogger(MessageListenerRegistrar.class);

    private ApplicationContext applicationContext;
    private final MessageConsumer messageConsumer;
    private MessagingProperties props;

    public MessageListenerRegistrar(ApplicationContext applicationContext,
                                    MessageConsumer consumer,
                                    MessagingProperties props) {
        this.applicationContext = applicationContext;
        this.messageConsumer = consumer;
        this.props = props;
    }

    @Override
    public void setApplicationContext(ApplicationContext ctx) throws BeansException {
        this.applicationContext = ctx;
    }

    /**
     * SmartInitializingSingleton 会在所有单例 Bean 创建完成后执行
     * 这是注册监听器的最佳时机
     */
    @Override
    public void afterSingletonsInstantiated() {
        String[] beanNames = applicationContext.getBeanNamesForType(Object.class);

        Arrays.stream(beanNames).forEach(beanName -> {
            Object bean = applicationContext.getBean(beanName);
            Class<?> targetClass = AopUtils.getTargetClass(bean);

            for (Method method : targetClass.getMethods()) {

                MessageListener ann = method.getAnnotation(MessageListener.class);
                if (ann == null) {
                    continue;
                }

                validateSignature(method);

                String subscriberId = buildSubscriberId(targetClass, method, ann);
                String destination = ann.destination();

                log.info("Registering MessageListener: bean={}, method={}, destination={}, subscriberId={}",
                        beanName, method.getName(), destination, subscriberId);

                messageConsumer.subscribe(
                        subscriberId,
                        Set.of(destination),
                        (MessageHandler) msg -> invokeListener(bean, method, msg)
                );
            }
        });
    }

    private void validateSignature(Method method) {
        Class<?>[] params = method.getParameterTypes();
        if (params.length != 1 || !Message.class.isAssignableFrom(params[0])) {
            throw new IllegalStateException(
                    "@MessageListener method must have exactly 1 parameter of type Message: " + method
            );
        }
    }

    private void invokeListener(Object bean, Method method, Message msg) {
        try {
            method.invoke(bean, msg);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke listener method: " + method, e);
        }
    }

    private String buildSubscriberId(Class<?> clazz, Method method, MessageListener ann) {
        if (!ann.subscriberId().isBlank()) {
            return ann.subscriberId();
        }
        return clazz.getSimpleName() + "." + method.getName();
    }
}
