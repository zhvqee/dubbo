package org.apache.dubbo.registry.mytest;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.registry.RegistryFactory;
import org.junit.jupiter.api.Test;

public class RegistryFactoryTest {
    public static void main(String[] args) {
        String decode = URL.decode("zookeeper://106.52.187.48:2181/org.apache.dubbo.registry.RegistryService?application=dubbo-demo-annotation-provider&dubbo=2.0.2&export=dubbo%3A%2F%2F192.168.0.105%3A20880%2Forg.apache.dubbo.demo.DemoService%3Fanyhost%3Dtrue%26application%3Ddubbo-demo-annotation-provider%26bind.ip%3D192.168.0.105%26bind.port%3D20880%26deprecated%3Dfalse%26dubbo%3D2.0.2%26dynamic%3Dtrue%26generic%3Dfalse%26interface%3Dorg.apache.dubbo.demo.DemoService%26metadata-type%3Dremote%26methods%3DsayHello%2CsayHelloAsync%26pid%3D9990%26release%3D%26side%3Dprovider%26timestamp%3D1596943034484&pid=9990&registry_protocol=zookeeper&timestamp=1596943034477");
        System.out.println(decode);
    }


    @Test
    public  void testRegistryFactryAdaptive(){

        ExtensionLoader<RegistryFactory> extensionLoader = ExtensionLoader.getExtensionLoader(RegistryFactory.class);
        RegistryFactory adaptiveExtension = extensionLoader.getAdaptiveExtension();
        System.out.println(adaptiveExtension);


    }
}
