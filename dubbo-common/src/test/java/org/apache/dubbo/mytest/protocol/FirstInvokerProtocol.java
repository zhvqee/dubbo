package org.apache.dubbo.mytest.protocol;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.mytest.InvokerProtocol;

public class FirstInvokerProtocol implements InvokerProtocol {
    @Override
    public Invoker getInvoker(String name, URL url) {
        Invoker invoker = new Invoker();
        invoker.setId(1L);
        return invoker;
    }
}
