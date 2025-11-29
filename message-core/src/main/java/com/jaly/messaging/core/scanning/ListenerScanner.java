package com.jaly.messaging.core.scanning;

import com.jaly.messaging.core.config.EasyMessagingProperties;
import com.jaly.messaging.core.consumer.MessageConsumer;
import com.jaly.messaging.core.consumer.MessageListener;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

public class ListenerScanner implements ApplicationContextAware, InitializingBean {

    private final MessageConsumer consumer;      // 具体 MQ 实现注入
    private final EasyMessagingProperties props; // destination 映射

    private ApplicationContext applicationContext;

    public ListenerScanner(MessageConsumer consumer, EasyMessagingProperties props) {
        this.consumer = consumer;
        this.props = props;
    }

    @Override
    public void afterPropertiesSet() {
        Map<String, Object> beans = applicationContext.getBeansWithAnnotation(Component.class);

        // 或直接扫所有 bean
        for (Object bean : beans.values()) {
            for (Method method : bean.getClass().getMethods()) {
                MessageListener ann = method.getAnnotation(MessageListener.class);
                if (ann == null) continue;

                String logicalDest = ann.destination();
                String realTopic = props.resolveDestination(logicalDest);

                String subscriberId = ann.subscriberId();
                if (subscriberId.isEmpty()) {
                    subscriberId = bean.getClass().getSimpleName() + "_" + method.getName();
                }

                // 注册消息消费逻辑
                consumer.subscribe(subscriberId, Set.of(realTopic), msg -> {
                    try {
                        method.invoke(bean, msg);
                    } catch (Exception e) {
                        //TODO: handle exception
                    }
                });
            }
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
