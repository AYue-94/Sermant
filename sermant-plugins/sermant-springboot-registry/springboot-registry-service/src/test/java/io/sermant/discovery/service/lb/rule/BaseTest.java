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

package io.sermant.discovery.service.lb.rule;

import io.sermant.core.config.ConfigManager;
import io.sermant.core.plugin.config.PluginConfigManager;
import io.sermant.core.plugin.service.PluginServiceManager;
import io.sermant.core.utils.ReflectUtils;
import io.sermant.discovery.config.DiscoveryPluginConfig;
import io.sermant.discovery.config.LbConfig;
import io.sermant.discovery.service.lb.discovery.zk.ZkClient;
import io.sermant.discovery.service.lb.utils.CommonUtils;

import org.apache.curator.framework.state.ConnectionState;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Basic test
 *
 * @author zhouss
 * @since 2022-10-09
 */
public class BaseTest {
    protected final LbConfig lbConfig = new LbConfig();

    protected MockedStatic<PluginServiceManager> pluginServiceManagerMockedStatic;

    protected MockedStatic<PluginConfigManager> pluginConfigManagerMockedStatic;

    protected MockedStatic<ConfigManager> configManagerMockedStatic;

    @Before
    public void setUp() {
        pluginConfigManagerMockedStatic = Mockito
                .mockStatic(PluginConfigManager.class);
        pluginConfigManagerMockedStatic.when(() -> PluginConfigManager.getPluginConfig(LbConfig.class))
                .thenReturn(lbConfig);
        pluginConfigManagerMockedStatic.when(() -> PluginConfigManager.getPluginConfig(DiscoveryPluginConfig.class))
                .thenReturn(new DiscoveryPluginConfig());
        pluginServiceManagerMockedStatic = Mockito
                .mockStatic(PluginServiceManager.class);
        configManagerMockedStatic = Mockito.mockStatic(ConfigManager.class);
    }

    @After
    public void tearDown() {
        CommonUtils.cleanServiceStats();
        pluginConfigManagerMockedStatic.close();
        pluginServiceManagerMockedStatic.close();
        configManagerMockedStatic.close();
    }

    /**
     * Get mocked zkclients
     *
     * @return ZkClient
     */
    protected ZkClient getClient() {
        final ZkClient zkClient = new ZkClient();
        zkClient.start();
        final Optional<Object> zkState = ReflectUtils.getFieldValue(zkClient, "zkState");
        Assert.assertTrue(zkState.isPresent() && zkState.get() instanceof AtomicReference);
        ((AtomicReference<ConnectionState>) zkState.get()).set(ConnectionState.CONNECTED);
        return zkClient;
    }
}
