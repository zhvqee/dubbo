package org.apache.dubbo.rpc.cluster;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;

public class Cluster$Adaptive {
    public Invoker join(Directory directory) throws RpcException {
        if (directory == null)
            throw new IllegalArgumentException("org.apache.dubbo.rpc.cluster.Directory argument == null");
        if (directory.getUrl() == null)
            throw new IllegalArgumentException("org.apache.dubbo.rpc.cluster.Directory argument getUrl() == null");
        URL url = directory.getUrl();
        String extName = url.getParameter("cluster", "failover");
        if(extName == null)
            throw new IllegalStateException("Failed to get extension (org.apache.dubbo.rpc.cluster.Cluster) name from url (" + url.toString() + ") use keys([cluster])");
        Cluster extension = ExtensionLoader.getExtensionLoader(Cluster.class).getExtension(extName);
        return extension.join(directory);
    }
}
