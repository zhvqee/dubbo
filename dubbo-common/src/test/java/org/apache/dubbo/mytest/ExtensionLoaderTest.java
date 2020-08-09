package org.apache.dubbo.mytest;

import com.sun.tools.javac.util.Assert;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.URLBuilder;
import org.apache.dubbo.common.extension.ExtensionFactory;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.mytest.protocol.AdaptiveInvokerProtocol;
import org.apache.dubbo.mytest.protocol.FirstInvokerProtocol;
import org.apache.dubbo.mytest.protocol.Invoker;
import org.junit.jupiter.api.Test;

import java.util.List;

public class ExtensionLoaderTest {

    public static void main(String[] args) {
        String decode = URL.decode("%3A%2F%2F172.18.254.38%3A20880%2Forg.apache.dubbo.demo.DemoService%3Fanyhost%3Dtrue%26application%3Ddubbo-demo-annotation-provider%26deprecated%3Dfalse%26dubbo%3D2.0.2%26dynamic%3Dtrue%26generic%3Dfalse%26interface%3Dorg.apache.dubbo.demo.DemoService%26metadata-type%3Dremote%26methods%3DsayHello%2CsayHelloAsync%26pid%3D5786%26release%3D%26side%3Dprovider%26timestamp%3D1596780352849");
        System.out.println(decode);
    }





    @Test
    public  void testDefaultExtension2(){
        ExtensionLoader<InvokerProtocol> extensionLoader = ExtensionLoader.getExtensionLoader(InvokerProtocol.class);

        InvokerProtocol firstInvokerProtocol = extensionLoader.getExtension("first");
        InvokerProtocol defaultExtension = extensionLoader.getDefaultExtension();
    }


    @Test
    public  void testDefaultExtension(){
        ExtensionLoader<InvokerProtocol> extensionLoader = ExtensionLoader.getExtensionLoader(InvokerProtocol.class);

        InvokerProtocol firstInvokerProtocol = extensionLoader.getExtension("first");
        Assert.check(firstInvokerProtocol instanceof FirstInvokerProtocol); //true

        InvokerProtocol defaultExtension = extensionLoader.getDefaultExtension();
        Assert.check(defaultExtension ==firstInvokerProtocol); //true
    }


    @Test
    public  void testActivateExtension(){
        ExtensionLoader<InvokerProtocol> extensionLoader = ExtensionLoader.getExtensionLoader(InvokerProtocol.class);

        URLBuilder urlBuilder = new URLBuilder();
        urlBuilder.setProtocol("test");
        urlBuilder.setPath("defaultInvokerProtocol");
        urlBuilder.addParameter("invokerProtocol", "first");
        List<InvokerProtocol> invokerProtocol = extensionLoader.getActivateExtension(urlBuilder.build(), "invokerProtocol");
        Assert.check(invokerProtocol.size() == 2);
    }

    /**
     * 1  @Activate  不能是默认的
     * 2  @Adaptive 可以用在方法上，并且该方法必须有个参数是URL类型
     */

    @Test
    public void testGetExtension() {
        ExtensionLoader<InvokerProtocol> extensionLoader = ExtensionLoader.getExtensionLoader(InvokerProtocol.class);

        InvokerProtocol defaultInvoerProtocal = extensionLoader.getExtension("first");
        Assert.check(defaultInvoerProtocal instanceof FirstInvokerProtocol);

        InvokerProtocol defaultExtension = extensionLoader.getDefaultExtension();
        Assert.check(defaultExtension instanceof FirstInvokerProtocol);

        URLBuilder urlBuilder = new URLBuilder();
        urlBuilder.setProtocol("test");
        urlBuilder.setPath("defaultInvokerProtocol");
        urlBuilder.addParameter("invokerProtocol", "second");
        List<InvokerProtocol> invokerProtocol = extensionLoader.getActivateExtension(urlBuilder.build(), "invokerProtocol");
        Assert.check(invokerProtocol.size() == 1);

     //   extensionLoader.addExtension("adaptive", AdaptiveInvokerProtocol.class);

        InvokerProtocol adaptiveExtension = extensionLoader.getAdaptiveExtension();
        Assert.check(adaptiveExtension != null);
        urlBuilder.addParameter("kk", "second");

        Invoker name = adaptiveExtension.getInvoker("name", urlBuilder.build());
        System.out.println(name);
    }



    @Test
    public  void testExtensionAdaptive(){

        InvokerProtocol adaptiveExtension = ExtensionLoader.getExtensionLoader(InvokerProtocol.class).getAdaptiveExtension();

        System.out.println(adaptiveExtension);
    }


    @Test
    public  void testExtensionFactory(){

        ExtensionFactory adaptiveExtension = ExtensionLoader.getExtensionLoader(ExtensionFactory.class).getAdaptiveExtension();

        System.out.println(adaptiveExtension);
    }
}
