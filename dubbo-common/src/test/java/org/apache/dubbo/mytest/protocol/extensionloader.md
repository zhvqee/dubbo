一、基础铺垫

1、@SPI 、@Activate、 @Adaptive

a、对于 @SPI,Dubbo默认的特性扩展接口，都必须打上这个@SPI，标识这是个Dubbo扩展点。如果自己需要新增dubbo的扩展点我们就需要新增接口，并且这个接口必须标注@SPI.

b、@SPI可以填入一个值，这个值代表某个扩展点的名称，一般作为默认扩展点。

c、@Activate，这个注解可以打在方法上或者类上，主要的作用是自动的激活某个扩展点。

d、@Adaptive 注解为自适应注解，该注解可以标注在方法上或者类上。这个注解特别有用，当它标注在类上时，说明该类是AdaptiveExtension扩展，如果标注在方法上，说明该方法可以根据参数自适应的返回值。需要注意的是该方法的参数必须有一个是URL类型。它是通过这个URL所带的参数进行自适应返回。当@Adaptive标注在方法上时，dubbo的spi扩展机制会动态生成一个XXXX$Adaptive扩展类，然后实现被@Adaptive标注的方法。内部的实现是通过ExtensionLoader.getActivateExtension 和传入的URL 来返回具体哪个扩展点实现。

2、ExtensionLoader

a、该方法就是我们对dubbo 扩展点的入口，他跟java的spi的ServiceLoader功能是一样的，都是加载某个扩展点的所有扩展具体实现。

b、ExtensionLoader 查找具体扩展点实现，是去查找类路径下 META-INF/dubbo, META-INF/dubbo/internal,META-INF/services目录下的扩展点接口名相同的文件。并加载文件内的具体扩展点。

c、入口是静态方法ExtensionLoader.getExtensionLoader()，返回某个扩展点的ExtensionLoader<?>的具体事例

d、ExtensionLoader<?>.getAdaptiveExtension 是得到自适应的扩展点类，如果某个类打上@Adaptive，则返回该类，否则系统通过javassit创建一个。

e、ExtensionLoader<?>.getActivateExtension(URL url,key) 可以根据URL.get(key)的值（该值就是扩展点名）得到一个激活的扩展点。注解 @Activate 标注的为默认的激活扩展点，可以通过-default来不激活默认扩展点。

f、ExtensionLoader<?>.getExtension 通过name 得到想要的扩展点，并且如果有Wrapper类型扩展点，会对其进行包裹返回。

g、当一个具体的扩展点的构造函数的参数是其扩展点接口类型，即构造函数是TypeImpl(IType type) && TypeImpl implements IType 时，可以称这种扩展点为Wraper扩展点。那么所返回的非Wraper扩展点都会被包裹成Wraper类型，如果有多个Wraper的扩展点，那么会一层一层的包裹，但是谁包裹谁，不固定。

h、当一个具体的扩展点的实现有其setter方法且没有被@DisableInject标注和注入类型不是原始类型(long,int,short,String,BigDecimal等)时，该setter方法会被调用，注入其需要的Object,而Object的实例原来就是通过ExtensionFactory工厂。

3、ExtensionFactory

a、首先ExtensionFactory 这个也是SPI的扩展点，它也是通过ExtensionLoader来加载的，他加载的时机是某个具体其他扩展点通过ExtensionLoader.getExtensionLoader()加载时，内部会调用ExtensionLoader私有构造函数，在这个构造函数内部来加载这个工厂，并且加载的这个工厂是自适应工程实例。

b、ExtensionLoader获取具体扩展点的来源就是通过ExtensionFactory扩展点的自适应扩展点AdaptiveExtensionFactory。

c、AdaptiveExtensionFactory 的实现可以知道扩展点的实例注入来源，包括Spring 容器(通过SpringExtensionFactory)和 SpiExtensionFactory。

二、特性测试

接着我们对上面的特性，进行一轮测试，验证其结果。
新建一个扩展接口

 package org.apache.dubbo.mytest;
 @SPI("first")
 public interface InvokerProtocol {
     @Adaptive("getInvoker")
     Invoker getInvoker(String name, URL url);
 }

默认的扩展点为first,并且getInvoker方法被@Adaptive标注。接着我们在META-INF/services 下新建一个文件名为org.apache.dubbo.mytest.InvokerProtocol文件，里面填写这个扩展点的实现，如：
first=org.apache.dubbo.mytest.protocol.FirstInvokerProtocol

测试1

@Test
 public  void testDefaultExtension(){
     ExtensionLoader<InvokerProtocol> extensionLoader = ExtensionLoader.getExtensionLoader(InvokerProtocol.class);
     InvokerProtocol firstInvokerProtocol = extensionLoader.getExtension("first");
     Assert.check(firstInvokerProtocol instanceof FirstInvokerProtocol); //true
     InvokerProtocol defaultExtension = extensionLoader.getDefaultExtension(); 
     Assert.check(defaultExtension ==firstInvokerProtocol); //true
 }
从上面我们验证了SPI("first")标注的名称，指定为默认的扩展实现类。接着我们在org.apache.dubbo.mytest.InvokerProtocol文件,接着新增一个扩展点，
second=org.apache.dubbo.mytest.protocol.SecondInvokerProtocol,并把SecondInvokerProtocol类打上@Activate("second")注解。注意，FirstInvokerProtocol没有打上@Activate注解。接着再一次测试

测试2

@Test
public  void testActivateExtension(){
    ExtensionLoader<InvokerProtocol> extensionLoader = ExtensionLoader.getExtensionLoader(InvokerProtocol.class);
    
    URLBuilder urlBuilder = new URLBuilder();
    urlBuilder.setProtocol("test");
    urlBuilder.setPath("defaultInvokerProtocol");
    urlBuilder.addParameter("invokerProtocol", "first");
    List<InvokerProtocol> invokerProtocol = extensionLoader.getActivateExtension(urlBuilder.build(), "invokerProtocol");
    Assert.check(invokerProtocol.size() == 2); //true
}

从该上面例子，验证了getActivateExtension 可以通过URL 来激活指定的扩展实现，并且还会返回被@Activate的扩展点类，说明@Activate被标注的扩展点类被默认激活。当我们在org.apache.dubbo.mytest.InvokerProtocol文件,接着新增二个扩展点，如下
firstwraper=org.apache.dubbo.mytest.protocol.FirstWraperInvokerProtocol
secondwraper=org.apache.dubbo.mytest.protocol.SecondWraperInvokerProtocol

这个2个扩展点是包裹扩展点，它的构造函数如下：

public FirstWraperInvokerProtocol(InvokerProtocol invokerProtocol) {
        this.invokerProtocol = invokerProtocol;
}
这时，我们运行test1的单测，发现断言失败，并且知道返回的扩展点实例被这2个wraper扩展点包裹。

测试3

@Test
 public  void testDefaultExtension(){
     ExtensionLoader<InvokerProtocol> extensionLoader = ExtensionLoader.getExtensionLoader(InvokerProtocol.class);
     InvokerProtocol firstInvokerProtocol = extensionLoader.getExtension("first");
     InvokerProtocol defaultExtension = extensionLoader.getDefaultExtension(); 
 }
从上面，得知firstInvokerProtocol 类型是FirstWraperInvokerProtocol 并且持有SecondWraperInvokerProtocol类型，而SecondWraperInvokerProtocol最终持有我们的FirstInvokerProtocol.

接着测试，我们获取下AdaptiveExtension扩展，目前我们只在扩展点的接口方法上打上@Adaptive，即getInvoker方法。

测试4

@Test
public  void testExtensionAdaptive(){
     InvokerProtocol adaptiveExtension = ExtensionLoader.getExtensionLoader(InvokerProtocol.class).getAdaptiveExtension();
     System.out.println(adaptiveExtension);
}
可以发现框架动态为我们生产了一个类名为InvokerProtocol$Adaptive的自适应扩展点类。并且实现其getInvoker方法，方法内容为(整理了下)。

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
我们知道其内部通过URL参数，根据url.getParameter得到我们我们需要的具体需要的扩展点名，接着ExtensionLoader.getExtension得到具体扩展点。

接着我们自己实现一个名为AdaptiveInvokerProtocol的自适应的扩展点类，并把AdaptiveInvokerProtocol标注上@Adaptive，接着在org.apache.dubbo.mytest.InvokerProtocol文件下，填入:
adaptive=org.apache.dubbo.mytest.protocol.AdaptiveInvokerProtocol
再一次执行

测试5

@Test
public  void testExtensionAdaptive(){
     InvokerProtocol adaptiveExtension = ExtensionLoader.getExtensionLoader(InvokerProtocol.class).getAdaptiveExtension();
     System.out.println(adaptiveExtension);
}
可以知道，自适应扩展点变为我们实现的AdaptiveInvokerProtocol，框架没有在为我们创建一个自适应的扩展点类。

三、源码跟踪

我们通过如果下几个问题来跟踪我们的代码。

1、为什么扩展点需要标注@SPI

我们在获取扩展点的入口为ExtensionLoader.getExtensionLoader。在这个静态方法里做了@SPI注解判断。

// 说明该接口上是否有SPI注解
 private static <T> boolean withExtensionAnnotation(Class<T> type) {
        return type.isAnnotationPresent(SPI.class);
    }
    @SuppressWarnings("unchecked")
    public static <T> ExtensionLoader<T> getExtensionLoader(Class<T> type) {
        if (type == null) {
            throw new IllegalArgumentException("Extension type == null");
        }
        // 扩展点必须为接口
        if (!type.isInterface()) {
            throw new IllegalArgumentException("Extension type (" + type + ") is not an interface!");
        }
        if (!withExtensionAnnotation(type)) {
            throw new IllegalArgumentException("Extension type (" + type +
                    ") is not an extension, because it is NOT annotated with @" + SPI.class.getSimpleName() + "!");
        }
        // 并为每种类型的扩展创建一个具体类型的ExtensionLoader<?>的实例，并放入EXTENSION_LOADERS缓存中。
        ExtensionLoader<T> loader = (ExtensionLoader<T>) EXTENSION_LOADERS.get(type);
        if (loader == null) {
            EXTENSION_LOADERS.putIfAbsent(type, new ExtensionLoader<T>(type));
            loader = (ExtensionLoader<T>) EXTENSION_LOADERS.get(type);
        }
        return loader;
    }
2、@Activate注解的作用如何实现的

我们通过 getActivateExtension 得到激活的扩展点，看下面的具体注释：

public List<T> getActivateExtension(URL url, String[] values, String group) {
        List<T> activateExtensions = new ArrayList<>();
        List<String> names = values == null ? new ArrayList<>(0) : asList(values);
        // 如果传入的值，没有包括-default，说明不排除框架，默认激活的扩展点
        if (!names.contains(REMOVE_VALUE_PREFIX + DEFAULT_KEY)) { 
            getExtensionClasses(); // 加载具体扩展点类，在加载过程中会把打上@Activate注解的类放到缓存cachedActivates中，其中key为扩展点名，Value为Activate注解
            for (Map.Entry<String, Object> entry : cachedActivates.entrySet()) {
                String name = entry.getKey();
                Object activate = entry.getValue();

                String[] activateGroup, activateValue;
                
                //得到Activate注解上的group和value.
                if (activate instanceof Activate) {
                    activateGroup = ((Activate) activate).group();
                    activateValue = ((Activate) activate).value();
                } else if (activate instanceof com.alibaba.dubbo.common.extension.Activate) {
                    activateGroup = ((com.alibaba.dubbo.common.extension.Activate) activate).group();
                    activateValue = ((com.alibaba.dubbo.common.extension.Activate) activate).value();
                } else {
                    continue;
                }
                //判断是否匹配，如果匹配加到activateExtensions。
                if (isMatchGroup(group, activateGroup)
                        && !names.contains(name)
                        && !names.contains(REMOVE_VALUE_PREFIX + name)
                        && isActive(activateValue, url)) {
                    activateExtensions.add(getExtension(name));
                }
            }
            activateExtensions.sort(ActivateComparator.COMPARATOR);
        }
        
        // 上面的if获取的是满足条件默认激活的扩展点类。这里是URL参数直接指定需要的扩展点放到loadedExtensions
        List<T> loadedExtensions = new ArrayList<>();
        for (int i = 0; i < names.size(); i++) {
            String name = names.get(i);
            
            if (!name.startsWith(REMOVE_VALUE_PREFIX)
                    && !names.contains(REMOVE_VALUE_PREFIX + name)) {
                if (DEFAULT_KEY.equals(name)) { 
                    if (!loadedExtensions.isEmpty()) {
                        activateExtensions.addAll(0, loadedExtensions);
                        loadedExtensions.clear();
                    }
                } else {
                    loadedExtensions.add(getExtension(name));
                }
            }
        }
        if (!loadedExtensions.isEmpty()) {
            activateExtensions.addAll(loadedExtensions);
        }
        // 接着返回默认的激活的扩展点+用户指定的扩展点。
        return activateExtensions;
    }
3、@Adaptive注解的作用如何实现的

自适应的扩展点实现类只能有一个，我们通过getAdaptiveExtension来获取，内容如下：

public T getAdaptiveExtension() {
        //首先cachedAdaptiveInstance用来存放自适应扩展点实例的Holder
        Object instance = cachedAdaptiveInstance.get();
        if (instance == null) { // 如果不存在
            if (createAdaptiveInstanceError != null) {
                throw new IllegalStateException("Failed to create adaptive instance: " +
                        createAdaptiveInstanceError.toString(),
                        createAdaptiveInstanceError);
            }

            synchronized (cachedAdaptiveInstance) { //锁住该Holder
                instance = cachedAdaptiveInstance.get();//再次获取，应为该方法可能是并发环境下，所以锁定双层判断
                if (instance == null) { 
                    try {
                        instance = createAdaptiveExtension(); //这里创建一个自适应扩展点实例
                        cachedAdaptiveInstance.set(instance);// 并放入Holder中
                    } catch (Throwable t) {
                        createAdaptiveInstanceError = t;
                        throw new IllegalStateException("Failed to create adaptive instance: " + t.toString(), t);
                    }
                }
            }
        }

        return (T) instance;
    }

接着我们来看下，这个createAdaptiveExtension是如何创建的。

private T createAdaptiveExtension() {
            try {
                // 步骤非常清晰，首先通过getAdaptiveExtensionClass得到自适应的扩展点的Class,然后调用newInstance得到一个实例
                //接着调用 injectExtension方法为这个实例注入相关需要的熟悉，通过这个实例的setter方法。
                return injectExtension((T) getAdaptiveExtensionClass().newInstance());
            } catch (Exception e) {
                throw new IllegalStateException("Can't create adaptive extension " + type + ", cause: " + e.getMessage(), e);
            }
        }
private Class<?> getAdaptiveExtensionClass() {
        getExtensionClasses(); // 这里还是通过getExtensionClasses加载扩展类，之后分析
        if (cachedAdaptiveClass != null) { //当调用完 getExtensionClasses方法后，如果存在自适应的扩展点类，会被赋值给cachedAdaptiveClass，那么直接返回，
            return cachedAdaptiveClass;
        }
        // 如果没有找到被@Adaptive标注的类，为其创建一个自适应的扩展点类
        return cachedAdaptiveClass = createAdaptiveExtensionClass();
    }
// 创建的一个自适应的扩展点的代码比较简单，就是StringBuilder 创建一个java类文件内容，然后通不过dubbo提供的compiler工具进行编译为字节码，通过javaassit完成。

private Class<?> createAdaptiveExtensionClass() {
        String code = new AdaptiveClassCodeGenerator(type, cachedDefaultName).generate();
        System.out.println(code);
        ClassLoader classLoader = findClassLoader();
        org.apache.dubbo.common.compiler.Compiler compiler = ExtensionLoader.getExtensionLoader(org.apache.dubbo.common.compiler.Compiler.class).getAdaptiveExtension();
        return compiler.compile(code, classLoader);
    }
4、ExtensionLoader是如何处理Wraper扩展点的

在通过getExtensionClasses 加载扩展点类时，会判断这个类时否是Wraper,判断如下：

private boolean isWrapperClass(Class<?> clazz) {
    try {
        clazz.getConstructor(type);
        return true;
    } catch (NoSuchMethodException e) {
        return false;
    }
}
即构造函数的参数否为自身扩展点，并把这些类放入一个名为cachedWrapperClasses的缓存中。

private void cacheWrapperClass(Class<?> clazz) {
        if (cachedWrapperClasses == null) {
            cachedWrapperClasses = new ConcurrentHashSet<>();
        }
        cachedWrapperClasses.add(clazz);
}
当我们调用ExtensionLoader<?>.getExtension 时，

public T getExtension(String name) {
          if (StringUtils.isEmpty(name)) {
              throw new IllegalArgumentException("Extension name == null");
          }
          // 如果名称为true，返回默认的扩展点
          if ("true".equals(name)) {
              return getDefaultExtension();
          }
          //从holder中得到名为name的扩展点
          final Holder<Object> holder = getOrCreateHolder(name);
          Object instance = holder.get();
          if (instance == null) {
              synchronized (holder) { //这里也是锁定双层判断
                  instance = holder.get();
                  if (instance == null) {
                      instance = createExtension(name); 
                      // 创建该扩展点实例，并放入该Holder中
                      holder.set(instance);
                  }
              }
          }
          return (T) instance;
}
private T createExtension(String name) {
              // 调用getExtensionClasses 得到扩展点类
              Class<?> clazz = getExtensionClasses().get(name);
              if (clazz == null) {
                  throw findException(name);
              }
              try {
                  T instance = (T) EXTENSION_INSTANCES.get(clazz);
                  if (instance == null) {
                      // 为空，clazz.newInstance()一个实力
                      EXTENSION_INSTANCES.putIfAbsent(clazz, clazz.newInstance());
                      instance = (T) EXTENSION_INSTANCES.get(clazz);
                  }
                  // 为扩展点setter注入
                  injectExtension(instance);
                  
                  // 这里,对包裹扩展点进行for循环，然后调用wrapperClass.getConstructor(type).newInstance ，一次循环包裹对象
                  Set<Class<?>> wrapperClasses = cachedWrapperClasses;
                  if (CollectionUtils.isNotEmpty(wrapperClasses)) {
                      for (Class<?> wrapperClass : wrapperClasses) {
                          instance = injectExtension((T) wrapperClass.getConstructor(type).newInstance(instance));
                      }
                  }
                  initExtension(instance);
                  return instance; //接着返回被包裹的实例
              } catch (Throwable t) {
                  throw new IllegalStateException("Extension instance (name: " + name + ", class: " +
                          type + ") couldn't be instantiated: " + t.getMessage(), t);
              }
}
5、ExtensionLoader是如何注入扩展点的属性的熟悉的注入是在，injectExtension中。

private T injectExtension(T instance) {
        if (objectFactory == null) {
            return instance;
        }

        try {
            for (Method method : instance.getClass().getMethods()) {
                // 判断是否为setter方法
                if (!isSetter(method)) {
                    continue;
                }
                /**
                 * Check {@link DisableInject} to see if we need auto injection for this property
                 */
                if (method.getAnnotation(DisableInject.class) != null) {
                    continue;
                }
                Class<?> pt = method.getParameterTypes()[0];
                // 如果是原始类型，则不需要注入，像String,Integer类型，是不知道需要注入什么的。
                if (ReflectUtils.isPrimitives(pt)) {
                    continue;
                }

                try {
                    String property = getSetterProperty(method);
                    // 从ExtensionFactory工程中，得到具体的需要注入的实例，
                    Object object = objectFactory.getExtension(pt, property);
                    if (object != null) {
                        // 接着调用setter方法，把属性注入到instance中
                        method.invoke(instance, object);
                    }
                } catch (Exception e) {
                    logger.error("Failed to inject via method " + method.getName()
                            + " of interface " + type.getName() + ": " + e.getMessage(), e);
                }

            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return instance;
    }
6、ExtensionFactory工厂在ExtensionLoader内部的运用

在injectExtension方法中，我们看到objectFactory.getExtension(pt, property)，即从objectFactory得到需要注入的属性对象。objectFactory 的实例化，我们是在ExtensionLoader私有构造函数中。

private ExtensionLoader(Class<?> type) {
        this.type = type;
        objectFactory = (type == ExtensionFactory.class ? null : ExtensionLoader.getExtensionLoader(ExtensionFactory.class).getAdaptiveExtension());
   }

而私有构造函数在getExtensionLoader的静态方法上调用。并且objectFactory还是自适应的扩展点实现。所以我们在加载一个扩展点时，首先框架内部会实例化这个ExtensionFactory工厂。并且这个自适应工程名为AdaptiveExtensionFactory,内容就是就是组合Spi和Spring的ExtensionFactory。

7、getExtensionClasses 的流程是什么。

流程比较清晰，不具体的贴代码了。简洁流程调用如下 ：
getExtensionClasses （这里是在缓存cachedClasses取，缓存取不到下一步） ->loadExtensionClasses（主要从META-INF/services,META-INF/dubbo,META-INF/dubbo/internal文件下找具体的扩展点类）
在这过程中把一些不同类型的扩展点放入到缓存中。具体看loadClass

private void loadClass(Map<String, Class<?>> extensionClasses, java.net.URL resourceURL, Class<?> clazz, String name,
                            boolean overridden) throws NoSuchMethodException {
         if (!type.isAssignableFrom(clazz)) {
             throw new IllegalStateException("Error occurred when loading extension class (interface: " +
                     type + ", class line: " + clazz.getName() + "), class "
                     + clazz.getName() + " is not subtype of interface.");
         }
         if (clazz.isAnnotationPresent(Adaptive.class)) {
             cacheAdaptiveClass(clazz, overridden);
         } else if (isWrapperClass(clazz)) {
             cacheWrapperClass(clazz);
         } else {
             clazz.getConstructor();
             if (StringUtils.isEmpty(name)) {
                 name = findAnnotationName(clazz);
                 if (name.length() == 0) {
                     throw new IllegalStateException("No such extension name for the class " + clazz.getName() + " in the config " + resourceURL);
                 }
             }
 
             String[] names = NAME_SEPARATOR.split(name);
             if (ArrayUtils.isNotEmpty(names)) {
                 cacheActivateClass(clazz, names[0]);
                 for (String n : names) {
                     cacheName(clazz, n);
                     saveInExtensionClass(extensionClasses, clazz, n, overridden);
                 }
             }
         }
     }
通过loadClass我们知道，ExtensionLoader内部缓存主要包括如下三个：

cachedClasses：存放没有被@Adaptive标注的且不是Wrapper扩展点类

cachedWrapperClasses ：存放Wrapper类型的扩展点类

cachedAdaptiveClass ：存放@Adaptive标注的类，没有被@Adaptive标注时，系统自动创建一个。


