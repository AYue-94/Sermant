/*
 * Copyright (C) 2023-2023 Huawei Technologies Co., Ltd. All rights reserved.
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

package com.huaweicloud.sermant.router.common.event;

import com.huaweicloud.sermant.core.event.EventLevel;
import com.huaweicloud.sermant.core.event.EventType;

/**
 * 路由插件事件定义
 *
 * @author lilai
 * @since 2023-03-28
 */
public enum RouterEventDefinition {
    /**
     * 路由插件规则刷新事件
     */

    ROUTER_RULE_REFRESH("ROUTER_RULE_REFRESH", EventType.OPERATION, EventLevel.NORMAL),

    /**
     * 同TAG优先规则匹配生效
     */
    SAME_TAG_RULE_MATCH("SAME_TAG_RULE_MATCH", EventType.GOVERNANCE, EventLevel.NORMAL),

    /**
     * 同TAG优先规则匹配失效
     */
    SAME_TAG_RULE_MISMATCH("SAME_TAG_RULE_MISMATCH", EventType.GOVERNANCE,
            EventLevel.NORMAL);

    private final String name;

    private final EventType eventType;

    private final EventLevel eventLevel;

    RouterEventDefinition(String name, EventType eventType, EventLevel eventLevel) {
        this.name = name;
        this.eventType = eventType;
        this.eventLevel = eventLevel;
    }

    public String getName() {
        return name;
    }

    public EventType getEventType() {
        return eventType;
    }

    public EventLevel getEventLevel() {
        return eventLevel;
    }

    /**
     * 获取事件触发区域
     *
     * @return 事件触发区域
     */
    public String getScope() {
        return "router-plugin";
    }
}
