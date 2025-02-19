/*
 * Copyright (C) 2024-2024 Sermant Authors. All rights reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package io.sermant.router.spring.utils;

import io.sermant.core.common.LoggerFactory;
import io.sermant.core.service.xds.entity.ServiceInstance;
import io.sermant.core.utils.CollectionUtils;
import io.sermant.core.utils.StringUtils;
import io.sermant.router.common.xds.XdsRouterHandler;
import io.sermant.router.common.xds.lb.XdsLoadBalancer;
import io.sermant.router.common.xds.lb.XdsLoadBalancerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * BaseHttpUtils
 *
 * @author daizhenyu
 * @since 2024-09-09
 **/
public class BaseHttpRouterUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger();

    private static final Pattern IP_PATTERN = Pattern.compile("^([0-9]{1,3}\\.){3}[0-9]{1,3}$");

    private static final String LOCAL_HOST = "localhost";

    private BaseHttpRouterUtils() {
    }

    /**
     * rebuild new url by XdsServiceInstance
     *
     * @param oldUrl old url
     * @param serviceInstance xds service instance
     * @return new url
     */
    public static String rebuildUrlByXdsServiceInstance(URL oldUrl, ServiceInstance serviceInstance) {
        try {
            return rebuildUrlByXdsServiceInstance(oldUrl.toURI(), serviceInstance);
        } catch (URISyntaxException e) {
            LOGGER.log(Level.WARNING, "Convert url to uri failed.", e.getMessage());
            return StringUtils.EMPTY;
        }
    }

    /**
     * rebuild new url by XdsServiceInstance
     *
     * @param oldUri old uri
     * @param serviceInstance xds service instance
     * @return new url
     */
    public static String rebuildUrlByXdsServiceInstance(URI oldUri, ServiceInstance serviceInstance) {
        StringBuilder builder = new StringBuilder();
        builder.append(oldUri.getScheme())
                .append("://")
                .append(serviceInstance.getHost())
                .append(":")
                .append(serviceInstance.getPort())
                .append(oldUri.getPath());
        String query = oldUri.getQuery();
        if (StringUtils.isEmpty(query)) {
            return builder.toString();
        }
        builder.append("?").append(query);
        return builder.toString();
    }

    /**
     * choose service instance by xds for http call
     *
     * @param serviceName service name
     * @param path path
     * @param headers headers
     * @return ServiceInstance
     */
    public static Optional<ServiceInstance> chooseServiceInstanceByXds(String serviceName, String path,
            Map<String, String> headers) {
        Set<ServiceInstance> serviceInstanceByXdsRoute = XdsRouterHandler.INSTANCE
                .getServiceInstanceByXdsRoute(serviceName, path, headers);
        if (serviceInstanceByXdsRoute.isEmpty()) {
            return Optional.empty();
        }
        List<ServiceInstance> serviceInstanceList = new ArrayList<>(serviceInstanceByXdsRoute);
        XdsLoadBalancer loadBalancer = XdsLoadBalancerFactory
                .getLoadBalancer(serviceInstanceList.get(0).getClusterName());
        return Optional.of(loadBalancer.selectInstance(serviceInstanceList));
    }

    /**
     * isXdsRouteRequired
     *
     * @param host host
     * @return isXdsRouteRequired
     */
    public static boolean isXdsRouteRequired(String host) {
        // if host is ip, so no xds routing required
        if (StringUtils.isEmpty(host) || host.equals(LOCAL_HOST) || IP_PATTERN.matcher(host).matches()) {
            return false;
        }
        return true;
    }

    /**
     * process headers, just get first value for every header
     *
     * @param headers headers
     * @return processed headers
     */
    public static Map<String, String> processHeaders(Map<String, List<String>> headers) {
        return headers.entrySet().stream()
                .collect(Collectors.toMap(
                        Entry::getKey,
                        entry -> CollectionUtils.isEmpty(entry.getValue()) ? "" : entry.getValue().get(0)
                ));
    }
}
