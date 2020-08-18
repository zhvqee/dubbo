package org.apache.dubbo.demo.provider;

import org.apache.dubbo.config.annotation.DubboService;
import org.apache.dubbo.demo.DemoService;

@DubboService(version="2.0.0")
public class DemoServiceImpl2 implements DemoService {
    @Override
    public String sayHello(String name) {
        System.out.println("version2");
        return "version2" + ":" + name;
    }
}
