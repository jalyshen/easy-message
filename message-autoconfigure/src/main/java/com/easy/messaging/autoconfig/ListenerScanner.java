package com.easy.messaging.autoconfig;

import com.easy.messaging.core.listener.MessageListener;
import com.easy.messaging.eventhub.EventHubListenerContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.reflect.Method;

/**
 * 扫描 Bean，解析 @MessageListener，并注册到 Container 中。
 */
public class ListenerScanner implements ApplicationContextAware, InitializingBean {

    private static final Logger logger = LoggerFactory.getLogger(ListenerScanner.class);

    private final EventHubListenerContainer container;

    private final MessagingProperties props;

    private ApplicationContext applicationContext;

    public ListenerScanner(EventHubListenerContainer container, MessagingProperties props) {
        this.container = container;
        this.props = props;
    }

    @Override
    public void afterPropertiesSet() {
        String[] beanNames = applicationContext.getBeanDefinitionNames();

        for (String beanName : beanNames) {
            Object bean = applicationContext.getBean(beanName);
            Class<?> targetClass = AopUtils.getTargetClass(bean);

            for (Method method : targetClass.getMethods()) {
                // 使用 AnnotationUtils 以支持代理
                MessageListener ann = AnnotationUtils.findAnnotation(method, MessageListener.class);
                if (ann == null) continue;

                String logicalDest = ann.destination();
                // 1. 解析 Topic
                String realTopic = props.resolveDestination(logicalDest);

                // 2. 找到该 Topic 对应的 Connection String 配置
                MessagingProperties.ConsumerInfo consumerInfo = props.findConsumerInfoByTopic(realTopic);

                if (consumerInfo == null) {
                    logger.warn("Found @MessageListener for destination '{}' (mapped to '{}') but no Consumer configuration found for this EventHub.",logicalDest, realTopic);
                    continue;
                }

                String subscriberId = ann.subscriberId();
                if (subscriberId.isEmpty()) {
                    subscriberId = props.getDefaults().getConsumer() != null ?
                            "easy-consumer-" + beanName : "default-consumer";
                }

                // 3. 注册到容器
                try {
                    container.registerListener(
                            consumerInfo.getConnectionString(),
                            consumerInfo.getEventHubName(),
                            consumerInfo.getConsumerGroup(),
                            msg -> {
                                try {
                                    method.invoke(bean, msg);
                                } catch (Exception e) {
                                    throw new RuntimeException("Invocation failed", e);
                                }
                            }
                    );
                    logger.info("Registered listener for destination [{}] -> topic [{}] on bean [{}]", logicalDest, realTopic, beanName);
                } catch (Exception e) {
                    logger.error("Failed to register listener for bean {}", beanName, e);
                }
            }
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
