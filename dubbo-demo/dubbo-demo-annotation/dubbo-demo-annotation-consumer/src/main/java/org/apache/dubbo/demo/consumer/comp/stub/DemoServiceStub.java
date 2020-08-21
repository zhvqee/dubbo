package org.apache.dubbo.demo.consumer.comp.stub;

import org.apache.dubbo.demo.DemoService;

import java.util.concurrent.CompletableFuture;

public class DemoServiceStub implements DemoService {


    private DemoService demoService;

    public DemoServiceStub(DemoService demoService) {
        this.demoService = demoService;
    }

    @Override
    public String sayHello(String name) {
        return demoService.sayHello(name);
    }

    @Override
    public CompletableFuture<String> sayHelloAsync(String name) {
        return demoService.sayHelloAsync(name);
    }
}
