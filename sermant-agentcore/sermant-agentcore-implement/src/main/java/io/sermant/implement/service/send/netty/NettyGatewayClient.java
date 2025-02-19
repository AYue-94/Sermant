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

package io.sermant.implement.service.send.netty;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;

import io.sermant.core.common.LoggerFactory;
import io.sermant.core.config.ConfigManager;
import io.sermant.core.service.send.api.GatewayClient;
import io.sermant.core.service.send.config.GatewayConfig;
import io.sermant.implement.service.send.netty.pojo.Message;
import io.sermant.implement.service.send.netty.pojo.Message.ServiceData.DataType;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Gateway sending service based on netty client
 *
 * @since 2022-03-26
 */
public class NettyGatewayClient implements GatewayClient {
    private static final Logger LOGGER = LoggerFactory.getLogger();

    private NettyClient nettyClient;

    @Override
    public void start() {
        GatewayConfig gatewayConfig = ConfigManager.getConfig(GatewayConfig.class);
        nettyClient = NettyClientFactory.getInstance()
                .getNettyClient(gatewayConfig.getNettyIp(), gatewayConfig.getNettyPort());
    }

    @Override
    public void stop() {
        NettyClientFactory.stop();
    }

    @Override
    public void send(byte[] data, int typeNum) {
        Optional<DataType> dataType = checkDataType(typeNum);
        if (!dataType.isPresent()) {
            return;
        }
        nettyClient.sendData(data, dataType.get());
    }

    @Override
    public void send(Object object, int typeNum) {
        Optional<DataType> dataType = checkDataType(typeNum);
        if (!dataType.isPresent()) {
            return;
        }
        nettyClient.sendData(
                JSON.toJSONString(object, SerializerFeature.WriteMapNullValue).getBytes(StandardCharsets.UTF_8),
                dataType.get());
    }

    @Override
    public boolean sendImmediately(byte[] data, int typeNum) {
        Optional<DataType> dataType = checkDataType(typeNum);
        return dataType.filter(type -> nettyClient.sendInstantData(data, type)).isPresent();
    }

    @Override
    public boolean sendImmediately(Object object, int typeNum) {
        Optional<DataType> dataType = checkDataType(typeNum);
        return dataType
                .filter(type -> nettyClient.sendInstantData(
                        JSON.toJSONString(object, SerializerFeature.WriteMapNullValue).getBytes(StandardCharsets.UTF_8),
                        type))
                .isPresent();
    }

    private Optional<DataType> checkDataType(int typeNum) {
        DataType dataType = Message.ServiceData.DataType.forNumber(typeNum);
        if (dataType == null) {
            LOGGER.severe("Cannot resolve this data type number: " + typeNum);
            return Optional.empty();
        }
        return Optional.of(dataType);
    }
}
