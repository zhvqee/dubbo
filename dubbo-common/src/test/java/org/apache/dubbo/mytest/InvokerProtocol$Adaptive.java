package org.apache.dubbo.mytest;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.mytest.protocol.Invoker;

public class InvokerProtocol$Adaptive implements InvokerProtocol {
    public Invoker getInvoker(String arg0, URL arg1) {
        if (arg1 == null)
            throw new IllegalArgumentException("url == null");
        URL url = arg1;
        String extName = url.getParameter("getInvoker", "first");
        if (extName == null)
            throw new IllegalStateException("Failed to get extension (org.apache.dubbo.mytest.InvokerProtocol) name " +
                    "from url (" + url.toString() + ") use keys([getInvoker])");
        InvokerProtocol extension = ExtensionLoader.getExtensionLoader(InvokerProtocol.class).getExtension(extName);
        return extension.getInvoker(arg0, arg1);
    }
}