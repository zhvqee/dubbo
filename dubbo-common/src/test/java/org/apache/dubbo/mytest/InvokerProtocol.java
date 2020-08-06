package org.apache.dubbo.mytest;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Adaptive;
import org.apache.dubbo.common.extension.SPI;
import org.apache.dubbo.mytest.protocol.Invoker;

@SPI("first")
public interface InvokerProtocol {

    @Adaptive("getInvoker")
    Invoker getInvoker(String name, URL url);

}
