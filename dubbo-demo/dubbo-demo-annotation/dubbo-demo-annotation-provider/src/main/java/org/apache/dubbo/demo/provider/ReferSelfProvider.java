package org.apache.dubbo.demo.provider;

import org.apache.dubbo.demo.DemoService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class ReferSelfProvider {

    @Resource
    private DemoService demoService;

    public void sayHello(String name) {
        demoService.sayHello(name);
    }
}
