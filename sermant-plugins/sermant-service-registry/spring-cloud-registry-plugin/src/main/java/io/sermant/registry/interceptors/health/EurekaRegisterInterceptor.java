/*
 * Copyright (C) 2022-2022 Huawei Technologies Co., Ltd. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.sermant.registry.interceptors.health;

import io.sermant.core.plugin.agent.entity.ExecuteContext;
import io.sermant.registry.config.RegisterDynamicConfig;
import io.sermant.registry.support.RegisterSwitchSupport;

/**
 * Eureka timer injection entry interception
 *
 * @author zhouss
 * @since 2022-04-11
 */
public class EurekaRegisterInterceptor extends RegisterSwitchSupport {
    @Override
    protected ExecuteContext doBefore(ExecuteContext context) {
        if (!registerConfig.isOpenMigration() || RegisterDynamicConfig.INSTANCE.isNeedCloseOriginRegisterCenter()) {
            context.skip(true);
        }
        return context;
    }
}
