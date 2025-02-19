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

package io.sermant.flowcontrol.retry.cluster;

import io.sermant.core.operation.OperationManager;
import io.sermant.core.operation.converter.api.YamlConverter;
import io.sermant.core.plugin.config.PluginConfigManager;
import io.sermant.core.service.ServiceManager;
import io.sermant.flowcontrol.common.config.CommonConst;
import io.sermant.flowcontrol.common.config.FlowControlConfig;
import io.sermant.flowcontrol.common.util.ConvertUtils;
import io.sermant.implement.operation.converter.YamlConverterImpl;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.AsyncRpcResult;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.Directory;
import org.apache.dubbo.rpc.cluster.loadbalance.RoundRobinLoadBalance;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.Arrays;

/**
 * Apache duubo cluster invoke test
 *
 * @author zhouss
 * @since 2022-09-14
 */
public class ApacheDubboClusterInvokerTest {
    private MockedStatic<PluginConfigManager> pluginConfigManagerMockedStatic;

    private MockedStatic<ServiceManager> serviceManagerMockedStatic;

    private MockedStatic<OperationManager> operationManagerMockedStatic;

    @After
    public void tearDown() {
        pluginConfigManagerMockedStatic.close();
        serviceManagerMockedStatic.close();
        operationManagerMockedStatic.close();
    }

    /**
     * preinitialization
     *
     * @throws Exception initialization failure thrown
     */
    @Before
    public void before() throws Exception {
        pluginConfigManagerMockedStatic = Mockito
                .mockStatic(PluginConfigManager.class);
        pluginConfigManagerMockedStatic.when(() -> PluginConfigManager.getPluginConfig(FlowControlConfig.class))
                .thenReturn(new FlowControlConfig());
        operationManagerMockedStatic = Mockito.mockStatic(OperationManager.class);
        operationManagerMockedStatic.when(() -> OperationManager.getOperation(YamlConverter.class)).thenReturn(new YamlConverterImpl());
        serviceManagerMockedStatic = Mockito.mockStatic(ServiceManager.class);
    }

    @Test
    public void doInvoke() {
        final Directory<Result> directory = Mockito.mock(Directory.class);
        Mockito.when(directory.getUrl()).thenReturn(new URL("dubbo", "localhost", 8080));
        final ApacheDubboClusterInvoker<Result> clusterInvoker = new ApacheDubboClusterInvoker<>(directory);
        final RoundRobinLoadBalance roundRobinLoadBalance = new RoundRobinLoadBalance();
        final Invocation invocation = Mockito.mock(Invocation.class);
        String interfaceName = this.getClass().getName();
        String version = "1.0.0";
        Mockito.when(invocation.getMethodName()).thenReturn("test");
        Mockito.when(invocation.getAttachment(ConvertUtils.DUBBO_ATTACHMENT_VERSION)).thenReturn(version);
        Mockito.when(invocation.getArguments()).thenReturn(new Object[]{"test"});
        final Invoker invoker = Mockito.mock(Invoker.class);
        Mockito.when(invoker.getInterface()).thenReturn(this.getClass());
        final URL url = Mockito.mock(URL.class);
        Mockito.when(url.getParameter(CommonConst.GENERIC_INTERFACE_KEY, interfaceName)).thenReturn(interfaceName);
        Mockito.when(url.getParameter(CommonConst.URL_VERSION_KEY, version)).thenReturn(version);
        Mockito.when(url.getParameter(CommonConst.DUBBO_REMOTE_APPLICATION)).thenReturn("application");
        Mockito.when(invoker.getUrl()).thenReturn(url);
        Mockito.when(invocation.getInvoker()).thenReturn(invoker);
        Mockito.when(directory.getUrl()).thenReturn(url);
        final AsyncRpcResult asyncRpcResult = AsyncRpcResult.newDefaultAsyncResult(new Object(), invocation);
        Mockito.when(invoker.invoke(invocation)).thenReturn(asyncRpcResult);
        final Result result = clusterInvoker.doInvoke(invocation, Arrays.asList(invoker), roundRobinLoadBalance);
        Assert.assertEquals(result, asyncRpcResult);
        // test throw exception
        Mockito.when(invoker.invoke(invocation)).thenThrow(new RpcException("test error"));
        boolean isEx = false;
        try {
            clusterInvoker.doInvoke(invocation, Arrays.asList(invoker), roundRobinLoadBalance);
        } catch (RpcException ex) {
            isEx = true;
        }
        Assert.assertTrue(isEx);
    }
}
