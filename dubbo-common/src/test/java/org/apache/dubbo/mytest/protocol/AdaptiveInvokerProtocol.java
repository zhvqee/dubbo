package org.apache.dubbo.mytest.protocol;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Adaptive;
import org.apache.dubbo.mytest.InvokerProtocol;

@Adaptive
public class AdaptiveInvokerProtocol implements InvokerProtocol {
    @Override
    public Invoker getInvoker(String name, URL url) {
        return null;
    }
}
