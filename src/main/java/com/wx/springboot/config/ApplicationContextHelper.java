package com.wx.springboot.config;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.support.AbstractRefreshableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

/**
 * spring工厂调用辅助类
 *
 * @author flyleft
 */
@Component
public class ApplicationContextHelper implements ApplicationContextAware {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationContextHelper.class);
    /**
     * 异步从 ApplicationContextHelper 获取 bean 对象并设置到目标对象
     * 调度任务的周期：单位秒
     * 默认：2
     */
    private static final long ASYNC_SETTER_SCHEDULE_PERIOD = 2;
    /**
     * 异步从 ApplicationContextHelper 获取 bean 对象并设置到目标对象
     * 设置bean报错时，最大尝试次数
     * 默认：10
     */
    private static final int ASYNC_SETTER_MAX_FAILURE_COUNT = 10;
    /**
     * 异步从 ApplicationContextHelper 获取 bean 对象并设置到目标对象
     * 当上下文对象为null时，最大尝试次数
     * 默认：9999
     * （即在9999 * 2 = 19998秒里上下文为空都会进行尝试处理）
     *
     * @see ApplicationContextHelper#ASYNC_SETTER_SCHEDULE_PERIOD
     */
    private static final int ASYNC_SETTER_MAX_CONTENT_IS_NULL_COUNT = 9999;

    private static DefaultListableBeanFactory springFactory;

    private static ApplicationContext context;

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) {
        ApplicationContextHelper.setContext(applicationContext);
        if (applicationContext instanceof AbstractRefreshableApplicationContext) {
            AbstractRefreshableApplicationContext springContext =
                    (AbstractRefreshableApplicationContext) applicationContext;
            ApplicationContextHelper.setFactory((DefaultListableBeanFactory) springContext.getBeanFactory());
        } else if (applicationContext instanceof GenericApplicationContext) {
            GenericApplicationContext springContext = (GenericApplicationContext) applicationContext;
            ApplicationContextHelper.setFactory(springContext.getDefaultListableBeanFactory());
        }
    }

    private static void setContext(ApplicationContext applicationContext) {
        ApplicationContextHelper.context = applicationContext;
    }

    private static void setFactory(DefaultListableBeanFactory springFactory) {
        ApplicationContextHelper.springFactory = springFactory;
    }

    public static DefaultListableBeanFactory getSpringFactory() {
        return springFactory;
    }

    public static ApplicationContext getContext() {
        return context;
    }

    public static <T> void asyncInstanceSetter(Class<T> type, Object target, String setterMethod) {
        asyncInstanceSetter(type, null, target, setterMethod);
    }

    /**
     * 异步从 ApplicationContextHelper 获取 bean 对象并设置到目标对象中，在某些启动期间需要初始化的bean可采用此方法。
     * <p>
     * 适用于实例方法注入。
     *
     * @param type         bean type
     * @param beanName     bean name
     * @param target       目标类对象
     * @param setterMethod setter 方法，target 中需包含此方法名，且类型与 type 一致
     * @param <T>          type
     */
    public static <T> void asyncInstanceSetter(Class<T> type, String beanName, Object target, String setterMethod) {
        asyncSetterStrategy(() -> setByMethod(type, beanName, target, setterMethod),
                () -> LOGGER.error("Setter field [{}] in [{}] failure because timeout.", setterMethod, target.getClass().getName()));
    }

    public static void asyncStaticSetter(Class<?> type, Class<?> target, String targetField) {
        asyncStaticSetter(type, null, target, targetField);
    }

    /**
     * 异步从 ApplicationContextHelper 获取 bean 对象并设置到目标对象中，在某些启动期间需要初始化的bean可采用此方法。
     * <br>
     * 一般可用于向静态类注入实例对象。
     *
     * @param type        bean type
     * @param target      目标类
     * @param targetField 目标字段
     */
    public static void asyncStaticSetter(Class<?> type, String beanName, Class<?> target, String targetField) {
        asyncSetterStrategy(() -> setByField(type, beanName, target, targetField),
                () -> LOGGER.error("Setter field [{}] in [{}] failure because timeout.", targetField, target.getName()));
    }

    private static boolean setByMethod(Class<?> type, String beanName, Object target, String targetMethod) {
        if (getContext() == null) {
            return false;
        }
        try {
            Object obj = getBean(type, beanName);
            Method method = target.getClass().getDeclaredMethod(targetMethod, type);
            method.setAccessible(true);
            method.invoke(target, obj);
            LOGGER.info("Async set field [{}] in [{}] success by method.",
                    targetMethod, target.getClass().getName());
            return true;
        } catch (NoSuchMethodException e) {
            LOGGER.error("Not found method [{}] in [{}].", targetMethod, target.getClass().getName());
        } catch (NoSuchBeanDefinitionException e) {
            LOGGER.error("Not found bean [{}] for [{}].", type.getName(), target.getClass().getName());
        } catch (Exception e) {
            LOGGER.error("Async set field [{}] in [{}] failure by method. exception: {}",
                    targetMethod, target.getClass().getName(), e.getMessage());
        }
        return false;
    }

    private static boolean setByField(Class<?> type, String beanName, Class<?> target, String targetField) {
        if (getContext() == null) {
            return false;
        }
        try {
            Object obj = getBean(type, beanName);
            Field field = target.getDeclaredField(targetField);
            field.setAccessible(true);
            field.set(target, obj);
            LOGGER.info("Async set field [{}] in [{}] success by field.",
                    targetField, target.getName());
            return true;
        } catch (NoSuchFieldException e) {
            LOGGER.error("Not found field [{}] in [{}].", targetField, target.getName(), e);
        } catch (NoSuchBeanDefinitionException e) {
            LOGGER.error("Not found bean [{}] for [{}].", type.getName(), target.getName(), e);
        } catch (Exception e) {
            LOGGER.error("Async set field [{}] in [{}] failure by field. exception: {}",
                    targetField, target.getName(), e.getMessage());
        }
        return false;
    }

    private static Object getBean(Class<?> type, String beanName) {
        Object obj;
        if (beanName != null) {
            try {
                obj = getContext().getBean(beanName, type);
            } catch (NoSuchBeanDefinitionException e) {
                obj = getContext().getBean(type);
                LOGGER.warn("getBean by beanName [{}] not found, then getBean by type.", beanName);
            }
        } else {
            obj = getContext().getBean(type);
        }
        return obj;
    }

    /**
     * 异步设置bean执行策略
     *
     * @param setter           设置器，返回设置结果，true 设置成功 false 设置失败
     * @param failureProcessor 处理失败结果处理器
     */
    private static void asyncSetterStrategy(@NonNull BooleanSupplier setter, @NonNull Runnable failureProcessor) {
        if (setter.getAsBoolean()) {
            return;
        }

        AtomicInteger contentIsNullCounter = new AtomicInteger(0);
        AtomicInteger failureCounter = new AtomicInteger(0);
        ScheduledExecutorService executorService = new ScheduledThreadPoolExecutor(1, r -> new Thread(r, "sync-setter"));
        executorService.scheduleAtFixedRate(() -> {
            if (ApplicationContextHelper.getContext() == null) {
                if (contentIsNullCounter.addAndGet(1) > ASYNC_SETTER_MAX_CONTENT_IS_NULL_COUNT) {
                    failureProcessor.run();
                    executorService.shutdown();
                } else {
                    return;
                }
            }

            // 处理逻辑
            boolean success = setter.getAsBoolean();
            if (success) {
                executorService.shutdown();
            } else {
                if (failureCounter.addAndGet(1) > ASYNC_SETTER_MAX_FAILURE_COUNT) {
                    failureProcessor.run();
                    executorService.shutdown();
                }
            }
        }, 0, ASYNC_SETTER_SCHEDULE_PERIOD, TimeUnit.SECONDS);
    }
}
