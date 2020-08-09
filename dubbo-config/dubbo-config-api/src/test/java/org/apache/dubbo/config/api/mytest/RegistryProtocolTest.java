package org.apache.dubbo.config.api.mytest;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.registry.Registry;
import org.apache.dubbo.registry.RegistryFactory;
import org.apache.dubbo.rpc.Protocol;
import org.junit.jupiter.api.Test;

public class RegistryProtocolTest {


    @Test
    public  void ss(){
        ExtensionLoader<Protocol> extensionLoader = ExtensionLoader.getExtensionLoader(Protocol.class);
        Protocol adaptiveExtension = extensionLoader.getAdaptiveExtension();
        System.out.println(adaptiveExtension);

    }

    @Test
    public  void testRegistry(){
        // 根据SPI 获取RegistryFactory 自适应注册工厂
        RegistryFactory registryFactory = ExtensionLoader.getExtensionLoader(RegistryFactory.class).getAdaptiveExtension();

        //通过url.getProtocol 和registryFactory得到 zookeeper注册中心
        URL registryUrl=URL.valueOf("zookeeper://127.0.0.1:2181/org.apache.dubbo.registry.RegistryService?application=dubbo-demo-annotation-provider&dubbo=2.0.2&export=dubbo://192.168.0.105:20880/org.apache.dubbo.demo.DemoService?anyhost=true&application=dubbo-demo-annotation-provider&bind.ip=192.168.0.105&bind.port=20880&deprecated=false&dubbo=2.0.2&dynamic=true&generic=false&interface=org.apache.dubbo.demo.DemoService&metadata-type=remote&methods=sayHello,sayHelloAsync&pid=9990&release=&side=provider&timestamp=1596943034484&pid=9990&registry_protocol=zookeeper&timestamp=1596943034477");
        Registry zookeeperRegistry = registryFactory.getRegistry(registryUrl);

        //根据zookeeperRegistry注册中心注册，需要的服务providerRegistryURL
        URL providerRegistryURL=URL.valueOf("dubbo://192.168.0.105:20880/org.apache.dubbo.demo.DemoService?anyhost=true&application=dubbo-demo-annotation-provider&deprecated=false&dubbo=2.0.2&dynamic=true&generic=false&interface=org.apache.dubbo.demo.DemoService&metadata-type=remote&methods=sayHello,sayHelloAsync&pid=9990&release=&side=provider&timestamp=1596943034484");
        zookeeperRegistry.register(providerRegistryURL);
    }
}
