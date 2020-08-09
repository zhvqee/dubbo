package org.apache.dubbo.registry.mytest;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.registry.Registry;
import org.apache.dubbo.registry.RegistryFactory;

public class RegistryFactory$Adaptive implements RegistryFactory {
    @Override
    public Registry getRegistry(URL url) {
        if (url == null) throw new IllegalArgumentException("url == null");
        String extName = (url.getProtocol() == null ? "dubbo" : url.getProtocol());
        if (extName == null)
            throw new IllegalStateException("Failed to get extension (org.apache.dubbo.registry.RegistryFactory) " +
                    "name from url (" + url.toString() + ") use keys([protocol])");
        RegistryFactory extension = ExtensionLoader.getExtensionLoader(RegistryFactory.class).getExtension(extName);
        return extension.getRegistry(url);
    }
}