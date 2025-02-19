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

package io.sermant.discovery.interceptors;

import io.sermant.core.common.LoggerFactory;
import io.sermant.core.plugin.agent.entity.ExecuteContext;
import io.sermant.core.plugin.service.PluginServiceManager;
import io.sermant.core.utils.LogUtils;
import io.sermant.core.utils.ReflectUtils;
import io.sermant.discovery.entity.ServiceInstance;
import io.sermant.discovery.retry.InvokerContext;
import io.sermant.discovery.service.InvokerService;
import io.sermant.discovery.utils.HttpConstants;
import io.sermant.discovery.utils.PlugEffectWhiteBlackUtils;
import io.sermant.discovery.utils.RequestInterceptorUtils;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response.Builder;

import org.apache.http.HttpStatus;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Blocks okhttp 3.x or later
 *
 * @author chengyouling
 * @since 2022-09-14
 */
public class OkHttp3ClientInterceptor extends MarkInterceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger();

    private static final String FIELD_NAME = "originalRequest";

    @Override
    protected ExecuteContext doBefore(ExecuteContext context) throws Exception {
        LogUtils.printHttpRequestBeforePoint(context);
        final InvokerService invokerService = PluginServiceManager.getPluginService(InvokerService.class);
        final Optional<Request> rawRequest = getRequest(context);
        if (!rawRequest.isPresent()) {
            return context;
        }
        Request request = rawRequest.get();
        URI uri = request.url().uri();
        if (!PlugEffectWhiteBlackUtils.isHostEqualRealmName(uri.getHost())) {
            return context;
        }
        Map<String, String> hostAndPath = RequestInterceptorUtils.recoverHostAndPath(uri.getPath());
        if (!PlugEffectWhiteBlackUtils.isPlugEffect(hostAndPath.get(HttpConstants.HTTP_URI_SERVICE))) {
            return context;
        }
        RequestInterceptorUtils.printRequestLog("OkHttp3", hostAndPath);
        AtomicReference<Request> rebuildRequest = new AtomicReference<>();
        rebuildRequest.set(request);
        invokerService.invoke(
                buildInvokerFunc(uri, hostAndPath, request, rebuildRequest, context),
                buildExFunc(rebuildRequest),
                hostAndPath.get(HttpConstants.HTTP_URI_SERVICE))
                .ifPresent(object -> setResultOrThrow(context, object, uri.getPath()));
        return context;
    }

    @Override
    protected void ready() {
    }

    private void setResultOrThrow(ExecuteContext context, Object result, String url) {
        if (result instanceof IOException) {
            LOGGER.log(Level.SEVERE, "Ok http client request error, uri is " + url, (Exception) result);
            context.setThrowableOut((Exception) result);
            return;
        }
        context.skip(result);
    }

    private Optional<Request> getRequest(ExecuteContext context) {
        final Optional<Object> originalRequest = ReflectUtils.getFieldValue(context.getObject(), FIELD_NAME);
        if (originalRequest.isPresent() && originalRequest.get() instanceof Request) {
            return Optional.of((Request) originalRequest.get());
        }
        return Optional.empty();
    }

    private Function<Throwable, Object> buildExFunc(AtomicReference<Request> rebuildRequest) {
        return ex -> buildErrorResponse(ex, rebuildRequest.get());
    }

    private Function<InvokerContext, Object> buildInvokerFunc(URI uri, Map<String, String> hostAndPath, Request request,
            AtomicReference<Request> rebuildRequest, ExecuteContext context) {
        return invokerContext -> {
            final String method = request.method();
            Request newRequest = covertRequest(uri, hostAndPath, request, method, invokerContext.getServiceInstance());
            rebuildRequest.set(newRequest);
            final Object target = copyNewCall(context, newRequest);
            return RequestInterceptorUtils.buildFunc(target, context.getMethod(), context.getArguments(),
                    invokerContext).get();
        };
    }

    private Object copyNewCall(ExecuteContext executeContext, Request newRequest) {
        final Optional<Object> client = ReflectUtils.getFieldValue(executeContext.getObject(), "client");
        if (!client.isPresent()) {
            return executeContext.getObject();
        }
        final OkHttpClient okHttpClient = (OkHttpClient) client.get();
        return okHttpClient.newCall(newRequest);
    }

    private Request covertRequest(URI uri, Map<String, String> hostAndPath, Request request, String method,
            ServiceInstance serviceInstance) {
        String url = RequestInterceptorUtils.buildUrlWithIp(uri, serviceInstance,
                hostAndPath.get(HttpConstants.HTTP_URI_PATH), method);
        return new Request.Builder()
                .headers(request.headers())
                .method(request.method(), request.body())
                .url(url)
                .tag(request.tag())
                .build();
    }

    /**
     * Build okHttp3 response
     *
     * @param request Request object
     * @param ex Specify an exception
     * @return response
     */
    private Object buildErrorResponse(Throwable ex, Request request) {
        if (ex instanceof IOException) {
            return ex;
        }
        Builder builder = new Builder();
        builder.code(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        builder.message(ex.getMessage());
        builder.protocol(Protocol.HTTP_1_1);
        builder.request(request);
        return builder.build();
    }

    @Override
    public ExecuteContext after(ExecuteContext context) throws Exception {
        LogUtils.printHttpRequestAfterPoint(context);
        return context;
    }

    @Override
    public ExecuteContext onThrow(ExecuteContext context) throws Exception {
        LogUtils.printHttpRequestOnThrowPoint(context);
        return context;
    }
}
