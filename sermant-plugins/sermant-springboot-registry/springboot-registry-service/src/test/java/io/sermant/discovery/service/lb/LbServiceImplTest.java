/*
 * Copyright (C) 2022-2022 Huawei Technologies Co., Ltd. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.sermant.discovery.service.lb;

import io.sermant.core.plugin.service.PluginServiceManager;
import io.sermant.core.utils.ReflectUtils;
import io.sermant.discovery.entity.ServiceInstance;
import io.sermant.discovery.service.LbService;
import io.sermant.discovery.service.ex.QueryInstanceException;
import io.sermant.discovery.service.lb.discovery.zk.ZkClient;
import io.sermant.discovery.service.lb.discovery.zk.ZkService34;
import io.sermant.discovery.service.lb.rule.BaseTest;
import io.sermant.discovery.service.lb.utils.CommonUtils;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Load balancing test
 *
 * @author zhouss
 * @since 2022-10-10
 */
public class LbServiceImplTest extends BaseTest {
    @Mock
    private ZkService34 zkService34;

    private final String serviceName = "discovery";

    private final LbService lbService = new LbServiceImpl();

    @Override
    public void setUp() {
        super.setUp();
        final ZkClient client = getClient();
        MockitoAnnotations.openMocks(this);
        pluginServiceManagerMockedStatic.when(() -> PluginServiceManager.getPluginService(ZkService34.class))
                .thenReturn(zkService34);
        pluginServiceManagerMockedStatic.when(() -> PluginServiceManager.getPluginService(ZkClient.class))
                .thenReturn(client);
        start();
    }

    @Override
    public void tearDown() {
        super.tearDown();
        // Reset the state
        final Optional<Object> isStarted = ReflectUtils.getFieldValue(DiscoveryManager.INSTANCE, "isStarted");
        Assert.assertTrue(isStarted.isPresent() && isStarted.get() instanceof AtomicBoolean);
        ((AtomicBoolean) isStarted.get()).set(false);
    }

    private List<ServiceInstance> mockInstances() {
        final List<ServiceInstance> serviceInstances = Arrays
                .asList(CommonUtils.buildInstance(serviceName, 8888), CommonUtils.buildInstance(serviceName, 8989));
        try {
            Mockito.when(zkService34.getInstances(serviceName)).thenReturn(serviceInstances);
        } catch (QueryInstanceException e) {
            // ignored
        }
        return serviceInstances;
    }

    private void start() {
        DiscoveryManager.INSTANCE.start();
        Mockito.verify(zkService34, Mockito.times(1)).init();
    }

    @Test
    public void choose() {
        final List<ServiceInstance> serviceInstances = mockInstances();
        final Optional<ServiceInstance> choose = lbService.choose(serviceName);
        Assert.assertTrue(choose.isPresent());
        Assert.assertTrue(serviceInstances.contains(choose.get()));
    }
}
