/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.dubbo.common.extension;

import org.apache.dubbo.event.Event;
import org.apache.dubbo.event.EventListener;
import org.apache.dubbo.registry.client.event.ServiceDiscoveryDestroyedEvent;
import org.apache.dubbo.registry.zookeeper.ZookeeperServiceDiscovery;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.fail;

public class ExtensionTest {

    private final ConcurrentMap<Class<? extends Event>, List<EventListener>> listenersCache = new ConcurrentHashMap<>();


    public Stream<EventListener> sortedListeners(Predicate<Map.Entry<Class<? extends Event>, List<EventListener>>> predicate) {
        return listenersCache
                .entrySet()
                .stream()
                .filter(predicate)
                .map(Map.Entry::getValue)
                .flatMap(Collection::stream)
                .sorted();
    }

    public static void main(String[] args) {


        ExtensionTest extensionTest = new ExtensionTest();
        ServiceDiscoveryDestroyedEvent event = new ServiceDiscoveryDestroyedEvent(new ZookeeperServiceDiscovery(), null);
        Stream<EventListener> eventListenerStream = extensionTest.sortedListeners(entry -> entry.getKey().isAssignableFrom(event.getClass()));
        List<EventListener> collect = eventListenerStream.collect(Collectors.toList());
        System.out.println(collect);
    }


    @Test
    public void testExtensionFactory() {
        try {
            ExtensionFactory factory = ExtensionLoader.getExtensionLoader(ExtensionFactory.class).getExtension("myfactory");
            Assertions.assertTrue(factory instanceof ExtensionFactory);
            Assertions.assertTrue(factory instanceof com.alibaba.dubbo.common.extension.ExtensionFactory);
            Assertions.assertTrue(factory instanceof MyExtensionFactory);

            ExtensionFactory spring = ExtensionLoader.getExtensionLoader(ExtensionFactory.class).getExtension("spring");
            Assertions.assertTrue(spring instanceof ExtensionFactory);
            Assertions.assertFalse(spring instanceof com.alibaba.dubbo.common.extension.ExtensionFactory);
        } catch (IllegalArgumentException expected) {
            fail();
        }
    }
}
