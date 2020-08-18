/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.config.spring.beans.factory.annotation;

import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.Reference;
import org.apache.dubbo.config.annotation.Service;
import org.apache.dubbo.config.spring.ReferenceBean;
import org.apache.dubbo.config.spring.ServiceBean;
import org.apache.dubbo.config.spring.context.event.ServiceBeanExportedEvent;

import com.alibaba.spring.beans.factory.annotation.AbstractAnnotationBeanPostProcessor;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.InjectionMetadata;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.core.annotation.AnnotationAttributes;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.alibaba.spring.util.AnnotationUtils.getAttribute;
import static com.alibaba.spring.util.AnnotationUtils.getAttributes;
import static java.lang.reflect.Proxy.newProxyInstance;
import static org.apache.dubbo.config.spring.beans.factory.annotation.ServiceBeanNameBuilder.create;
import static org.springframework.util.StringUtils.hasText;

/**
 * {@link org.springframework.beans.factory.config.BeanPostProcessor} implementation
 * that Consumer service {@link Reference} annotated fields
 *  处理 DubboReference 注册的BeanPostProcessor
 * @see DubboReference
 * @see Reference
 * @see com.alibaba.dubbo.config.annotation.Reference
 * @since 2.5.7
 */
public class ReferenceAnnotationBeanPostProcessor extends AbstractAnnotationBeanPostProcessor implements
        ApplicationContextAware, ApplicationListener<ServiceBeanExportedEvent> {

    /**
     * The bean name of {@link ReferenceAnnotationBeanPostProcessor}
     */
    public static final String BEAN_NAME = "referenceAnnotationBeanPostProcessor";

    /**
     * Cache size
     */
    private static final int CACHE_SIZE = Integer.getInteger(BEAN_NAME + ".cache.size", 32);

    /**
     *
     * 用来保存ReferenceBean(即被DubboReference注解打上的类的信息收集,每个被DubboReference，都会生成一个ReferecenBean)
     * key: ReferenceBeanName , value:ReferenceBean
     */
    private final ConcurrentMap<String, ReferenceBean<?>> referenceBeanCache =
            new ConcurrentHashMap<>(CACHE_SIZE);

    /**
     *
     *  需要注入的字段，value 是对应的ReferenceBean
     */
    private final ConcurrentMap<InjectionMetadata.InjectedElement, ReferenceBean<?>> injectedFieldReferenceBeanCache =
            new ConcurrentHashMap<>(CACHE_SIZE);

    /**
     *
     * 需要注入的方法，value 是对应的ReferenceBean
     */
    private final ConcurrentMap<InjectionMetadata.InjectedElement, ReferenceBean<?>> injectedMethodReferenceBeanCache =
            new ConcurrentHashMap<>(CACHE_SIZE);

    private final ConcurrentMap<String, ReferencedBeanInvocationHandler> referencedBeanInvocationHandlersCache =
            new ConcurrentHashMap<>();

    private ApplicationContext applicationContext;

    /**
     * {@link com.alibaba.dubbo.config.annotation.Reference @com.alibaba.dubbo.config.annotation.Reference} has been supported since 2.7.3
     * <p>
     * {@link DubboReference @DubboReference} has been supported since 2.7.7
     */
    public ReferenceAnnotationBeanPostProcessor() {
        super(DubboReference.class, Reference.class, com.alibaba.dubbo.config.annotation.Reference.class);
    }

    /**
     * Gets all beans of {@link ReferenceBean}
     *
     * @return non-null read-only {@link Collection}
     * @since 2.5.9
     */
    public Collection<ReferenceBean<?>> getReferenceBeans() {
        return referenceBeanCache.values();
    }

    /**
     * Get {@link ReferenceBean} {@link Map} in injected field.
     *
     * @return non-null {@link Map}
     * @since 2.5.11
     */
    public Map<InjectionMetadata.InjectedElement, ReferenceBean<?>> getInjectedFieldReferenceBeanMap() {
        return Collections.unmodifiableMap(injectedFieldReferenceBeanCache);
    }

    /**
     * Get {@link ReferenceBean} {@link Map} in injected method.
     *
     * @return non-null {@link Map}
     * @since 2.5.11
     */
    public Map<InjectionMetadata.InjectedElement, ReferenceBean<?>> getInjectedMethodReferenceBeanMap() {
        return Collections.unmodifiableMap(injectedMethodReferenceBeanCache);
    }

    /**
     *
     *
     * 该方法是个模板方法，用来得到一个需要注入类型的的对象。
     *
     * @param attributes ReferenceBean注解属性
     * @param bean  需要被注入的对象,一般是Spring Bean
     * @param beanName  需要被注入的对象名
     * @param injectedType  注入对象的类型
     * @param injectedElement  注入对象描述元信息
     * @return
     * @throws Exception
     */

    @Override
    protected Object doGetInjectedBean(AnnotationAttributes attributes, Object bean, String beanName, Class<?> injectedType,
                                       InjectionMetadata.InjectedElement injectedElement) throws Exception {
        /**
         * The name of bean that annotated Dubbo's {@link Service @Service} in local Spring {@link ApplicationContext}
         */
        /**
         *
         * 得到需要被注入的对象的BeanName,生成规则默认是,查看ServiceBeanNameBuilder.build()
         * ServiceBean:${interfaceName}:${version}:${group}
         */
        String referencedBeanName = buildReferencedBeanName(attributes, injectedType);

        /**
         * The name of bean that is declared by {@link Reference @Reference} annotation injection
         */
        /**
         * 得到引用对象@ReferenceBean的BeanName ,
         * 如果有Id 就是Id,没有通过generateReferenceBeanName()产生，产生的类名如下：
         * @Reference(${attributes})${interface}
         */
        String referenceBeanName = getReferenceBeanName(attributes, injectedType);

        /**
         * 构建一个ReferenceBean 对象，如果不存在的话。
         *
         */
        ReferenceBean referenceBean = buildReferenceBeanIfAbsent(referenceBeanName, attributes, injectedType);

        /**
         * 判断是否为本地ServiceBean,一般都是远程引用dubbo服务。
         */
        boolean localServiceBean = isLocalServiceBean(referencedBeanName, referenceBean, attributes);

        /**
         * 然后注册referenceBean
         */
        registerReferenceBean(referencedBeanName, referenceBean, attributes, localServiceBean, injectedType);

        /**
         * 把referenceBean 放入缓存中。
         */
        cacheInjectedReferenceBean(referenceBean, injectedElement);

        /**
         *
         * 通过referenceBean 创建动态代理创建一个injectedType类型的对象。核心，创建一个代理对象，用来代表需要引用远程的Service服务
         */
        return getOrCreateProxy(referencedBeanName, referenceBean, localServiceBean, injectedType);
    }

    /**
     * Register an instance of {@link ReferenceBean} as a Spring Bean
     *
     * 注册referenceBean 到Spring 容器中
     *
     * @param referencedBeanName  被@DubboService 标注的服务提供者类名
     * @param referenceBean      ReferenceBean 对象，由buildReferenceBeanIfAbsent创建
     * @param attributes        @Reference 注解属性
     * @param localServiceBean    就是说当前需要注入的类型是否在本地，就是在同一个jvm中，
     * @param interfaceClass     服务接口
     * @since 2.7.3
     */
    private void registerReferenceBean(String referencedBeanName, ReferenceBean referenceBean,
                                       AnnotationAttributes attributes,
                                       boolean localServiceBean, Class<?> interfaceClass) {

        //得到spring 的工厂
        ConfigurableListableBeanFactory beanFactory = getBeanFactory();

        /**
         *
         * 首先得到ReferenceBean 的名称
         */
        String beanName = getReferenceBeanName(attributes, interfaceClass);

        /**
         * 如果引用的dubbo服务在同一个jvm 上，直接从BeanFacory工厂上拿到
         */
        if (localServiceBean) {  // If @Service bean is local one
            /**
             * Get  the @Service's BeanDefinition from {@link BeanFactory}
             * Refer to {@link ServiceAnnotationBeanPostProcessor#buildServiceBeanDefinition}
             */
            /**
             * 从工厂中得到服务提供者的BeanDefinnition
             */
            AbstractBeanDefinition beanDefinition = (AbstractBeanDefinition) beanFactory.getBeanDefinition(referencedBeanName);

            /**
             *
             * 然后得到其真正引用的内容，即被@DubboService打上注解的类
             */
            RuntimeBeanReference runtimeBeanReference = (RuntimeBeanReference) beanDefinition.getPropertyValues().get("ref");
            // The name of bean annotated @Service
            /**
             * 然后得到这个服务实现类的BeanName
             */
            String serviceBeanName = runtimeBeanReference.getBeanName();
            // register Alias rather than a new bean name, in order to reduce duplicated beans
            /**
             *  因为本地就已经存在服务提供者，所以直接类名注册关联即可。
             */
            beanFactory.registerAlias(serviceBeanName, beanName);
        } else { // Remote @Service Bean
            /**
             * 如果引用的服务是在远程，并且未注册，则注册referenceBean。
             */
            if (!beanFactory.containsBean(beanName)) {
                beanFactory.registerSingleton(beanName, referenceBean);
            }
        }
    }

    /**
     * Get the bean name of {@link ReferenceBean} if {@link Reference#id() id attribute} is present,
     * or {@link #generateReferenceBeanName(AnnotationAttributes, Class) generate}.
     *
     * @param attributes     the {@link AnnotationAttributes attributes} of {@link Reference @Reference}
     * @param interfaceClass the {@link Class class} of Service interface
     * @return non-null
     * @since 2.7.3
     */
    private String getReferenceBeanName(AnnotationAttributes attributes, Class<?> interfaceClass) {
        // id attribute appears since 2.7.3
        String beanName = getAttribute(attributes, "id");
        if (!hasText(beanName)) {
            beanName = generateReferenceBeanName(attributes, interfaceClass);
        }
        return beanName;
    }

    /**
     * Build the bean name of {@link ReferenceBean}
     *
     * @param attributes     the {@link AnnotationAttributes attributes} of {@link Reference @Reference}
     * @param interfaceClass the {@link Class class} of Service interface
     * @return
     * @since 2.7.3
     */
    private String generateReferenceBeanName(AnnotationAttributes attributes, Class<?> interfaceClass) {
        StringBuilder beanNameBuilder = new StringBuilder("@Reference");

        if (!attributes.isEmpty()) {
            beanNameBuilder.append('(');
            for (Map.Entry<String, Object> entry : attributes.entrySet()) {
                beanNameBuilder.append(entry.getKey())
                        .append('=')
                        .append(entry.getValue())
                        .append(',');
            }
            // replace the latest "," to be ")"
            beanNameBuilder.setCharAt(beanNameBuilder.lastIndexOf(","), ')');
        }

        beanNameBuilder.append(" ").append(interfaceClass.getName());

        return beanNameBuilder.toString();
    }

    /**
     * Is Local Service bean or not?
     *
     * @param referencedBeanName the bean name to the referenced bean
     * @return If the target referenced bean is existed, return <code>true</code>, or <code>false</code>
     * @since 2.7.6
     */
    private boolean isLocalServiceBean(String referencedBeanName, ReferenceBean referenceBean, AnnotationAttributes attributes) {
        return existsServiceBean(referencedBeanName) && !isRemoteReferenceBean(referenceBean, attributes);
    }

    /**
     * Check the {@link ServiceBean} is exited or not
     *
     * @param referencedBeanName the bean name to the referenced bean
     * @return if exists, return <code>true</code>, or <code>false</code>
     * @revised 2.7.6
     */
    private boolean existsServiceBean(String referencedBeanName) {
        return applicationContext.containsBean(referencedBeanName) &&
                applicationContext.isTypeMatch(referencedBeanName, ServiceBean.class);

    }

    private boolean isRemoteReferenceBean(ReferenceBean referenceBean, AnnotationAttributes attributes) {
        boolean remote = Boolean.FALSE.equals(referenceBean.isInjvm()) || Boolean.FALSE.equals(attributes.get("injvm"));
        return remote;
    }

    /**
     * Get or Create a proxy of {@link ReferenceBean} for the specified the type of Dubbo service interface
     *
     * @param referencedBeanName   The name of bean that annotated Dubbo's {@link Service @Service} in the Spring {@link ApplicationContext}
     * @param referenceBean        the instance of {@link ReferenceBean}
     * @param localServiceBean     Is Local Service bean or not
     * @param serviceInterfaceType the type of Dubbo service interface
     * @return non-null
     * @since 2.7.4
     */
    private Object getOrCreateProxy(String referencedBeanName, ReferenceBean referenceBean, boolean localServiceBean,
                                    Class<?> serviceInterfaceType) {
        /**
         *
         * 如果是本地就有服务Bean的话。
         */
        if (localServiceBean) { // If the local @Service Bean exists, build a proxy of Service
            //通过Proxy.newProxyInstance创建一个新的代理对象，在内部通过applicationContext获取本地Service即可。
            return newProxyInstance(getClassLoader(), new Class[]{serviceInterfaceType},
                    newReferencedBeanInvocationHandler(referencedBeanName));
        } else {
            //如果是远程Service,判断被引用的服务referencedBeanName是否已经存在在applicationContext上，是的话，直接暴露服务，
            // 基本上不太可能，因为在前面已经判断了被引用的服务Bean在远程，所以这里仅仅是为了防止localServiceBean判断错误。
            exportServiceBeanIfNecessary(referencedBeanName); // If the referenced ServiceBean exits, export it immediately
            //接着直接通过get()方法来创建一个代理，这get操作就会引入dubbo服务的订阅等相关内容。
            return referenceBean.get();
        }
    }


    private void exportServiceBeanIfNecessary(String referencedBeanName) {
        if (existsServiceBean(referencedBeanName)) {
            ServiceBean serviceBean = getServiceBean(referencedBeanName);
            if (!serviceBean.isExported()) {
                serviceBean.export();
            }
        }
    }

    private ServiceBean getServiceBean(String referencedBeanName) {
        return applicationContext.getBean(referencedBeanName, ServiceBean.class);
    }

    private InvocationHandler newReferencedBeanInvocationHandler(String referencedBeanName) {
        return referencedBeanInvocationHandlersCache.computeIfAbsent(referencedBeanName,
                ReferencedBeanInvocationHandler::new);
    }

    /**
     * The {@link InvocationHandler} class for the referenced Bean
     */
    @Override
    public void onApplicationEvent(ServiceBeanExportedEvent event) {
        initReferencedBeanInvocationHandler(event.getServiceBean());
    }

    private void initReferencedBeanInvocationHandler(ServiceBean serviceBean) {
        String serviceBeanName = serviceBean.getBeanName();
        referencedBeanInvocationHandlersCache.computeIfPresent(serviceBeanName, (name, handler) -> {
            handler.init();
            return null;
        });
    }

    private class ReferencedBeanInvocationHandler implements InvocationHandler {

        private final String referencedBeanName;

        private Object bean;

        private ReferencedBeanInvocationHandler(String referencedBeanName) {
            this.referencedBeanName = referencedBeanName;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Object result = null;
            try {
                if (bean == null) {
                    init();
                }
                result = method.invoke(bean, args);
            } catch (InvocationTargetException e) {
                // re-throws the actual Exception.
                throw e.getTargetException();
            }
            return result;
        }

        private void init() {
            ServiceBean serviceBean = applicationContext.getBean(referencedBeanName, ServiceBean.class);
            this.bean = serviceBean.getRef();
        }
    }

    @Override
    protected String buildInjectedObjectCacheKey(AnnotationAttributes attributes, Object bean, String beanName,
                                                 Class<?> injectedType, InjectionMetadata.InjectedElement injectedElement) {
        return buildReferencedBeanName(attributes, injectedType) +
                "#source=" + (injectedElement.getMember()) +
                "#attributes=" + getAttributes(attributes, getEnvironment());
    }

    /**
     * @param attributes           the attributes of {@link Reference @Reference}
     * @param serviceInterfaceType the type of Dubbo's service interface
     * @return The name of bean that annotated Dubbo's {@link Service @Service} in local Spring {@link ApplicationContext}
     */
    private String buildReferencedBeanName(AnnotationAttributes attributes, Class<?> serviceInterfaceType) {
        ServiceBeanNameBuilder serviceBeanNameBuilder = create(attributes, serviceInterfaceType, getEnvironment());
        return serviceBeanNameBuilder.build();
    }

    private ReferenceBean buildReferenceBeanIfAbsent(String referenceBeanName, AnnotationAttributes attributes,
                                                     Class<?> referencedType)
            throws Exception {

        /**
         * 首先判断缓存上是否有
         */
        ReferenceBean<?> referenceBean = referenceBeanCache.get(referenceBeanName);

        if (referenceBean == null) {
            //没有的话通过ReferenceBeanBuilder 创建一个referenceBean
            ReferenceBeanBuilder beanBuilder = ReferenceBeanBuilder
                    .create(attributes, applicationContext)
                    .interfaceClass(referencedType);
            referenceBean = beanBuilder.build();
            referenceBeanCache.put(referenceBeanName, referenceBean);
            // 如果存在的话，需要怕判断ReferenceBean 对象持有的interfaceClass 是不是referencedType的类型或者子类型，不是抛异常。
        } else if (!referencedType.isAssignableFrom(referenceBean.getInterfaceClass())) {
            throw new IllegalArgumentException("reference bean name " + referenceBeanName + " has been duplicated, but interfaceClass " +
                    referenceBean.getInterfaceClass().getName() + " cannot be assigned to " + referencedType.getName());
        }
        return referenceBean;
    }

    private void cacheInjectedReferenceBean(ReferenceBean referenceBean,
                                            InjectionMetadata.InjectedElement injectedElement) {
        /**
         *
         * 通过@DubboReference 是标注在成员上还是方法上，分别缓存
         */
        if (injectedElement.getMember() instanceof Field) {
            injectedFieldReferenceBeanCache.put(injectedElement, referenceBean);
        } else if (injectedElement.getMember() instanceof Method) {
            injectedMethodReferenceBeanCache.put(injectedElement, referenceBean);
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void destroy() throws Exception {
        super.destroy();
        this.referenceBeanCache.clear();
        this.referencedBeanInvocationHandlersCache.clear();
        this.injectedFieldReferenceBeanCache.clear();
        this.injectedMethodReferenceBeanCache.clear();
    }
}
