/*
 * Copyright (C) 2022-2022 Huawei Technologies Co., Ltd. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package io.sermant.flowcontrol.res4j.util;

import io.sermant.core.plugin.config.PluginConfigManager;
import io.sermant.flowcontrol.common.config.FlowControlConfig;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * monitoring tool test class
 *
 * @author zhp
 * @since 2022-09-20
 */
public class MonitorUtilsTest {
    @Test
    public void testSwitch() {
        FlowControlConfig metricConfig = new FlowControlConfig();
        metricConfig.setEnableStartMonitor(true);
        try (MockedStatic<PluginConfigManager> pluginConfigManagerMockedStatic =
                     Mockito.mockStatic(PluginConfigManager.class)) {
            pluginConfigManagerMockedStatic.when(() -> PluginConfigManager.getPluginConfig(FlowControlConfig.class))
                    .thenReturn(metricConfig);
            Assert.assertTrue(MonitorUtils.isStartMonitor());
        }
    }
}
