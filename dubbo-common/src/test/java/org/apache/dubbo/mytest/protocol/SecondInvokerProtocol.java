package org.apache.dubbo.mytest.protocol;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.mytest.InvokerProtocol;

@Activate("second")
public class SecondInvokerProtocol implements InvokerProtocol {

    @Override
    public Invoker getInvoker(String name, URL url) {
        Invoker invoker = new Invoker();
        invoker.setId(2L);
        return invoker;
    }
}
