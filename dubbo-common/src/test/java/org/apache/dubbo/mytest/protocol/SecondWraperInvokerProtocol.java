package org.apache.dubbo.mytest.protocol;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.mytest.InvokerProtocol;

public class SecondWraperInvokerProtocol implements InvokerProtocol {

    private InvokerProtocol invokerProtocol;

    public SecondWraperInvokerProtocol(InvokerProtocol invokerProtocol) {
        this.invokerProtocol = invokerProtocol;
    }

    @Override
    public Invoker getInvoker(String name, URL url) {
        return invokerProtocol.getInvoker(name,url);
    }
}
