package org.apache.dubbo.demo;

import org.apache.dubbo.demo.DemoService;

public class DemoServiceMock implements DemoService {
    @Override
    public String sayHello(String name) {
        return "mock hello world";
    }
}
