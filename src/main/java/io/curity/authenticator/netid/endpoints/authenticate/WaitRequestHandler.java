/*
 *  Copyright 2022 Curity AB
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.curity.authenticator.netid.endpoints.authenticate;

import com.google.common.collect.ImmutableMap;
import io.curity.authenticator.netid.NetIdAccessServerSoapClient;
import io.curity.authenticator.netid.client.NetIdAccessClient;
import io.curity.authenticator.netid.client.WebServicePoller;
import io.curity.authenticator.netid.model.PollerPaths;
import io.curity.authenticator.netid.model.WaitRequestModel;
import io.curity.authenticator.netid.model.WaitResponseModel;
import io.curity.authenticator.netid.config.NetIdAccessConfig;
import se.curity.identityserver.sdk.authentication.AuthenticatedState;
import se.curity.identityserver.sdk.authentication.AuthenticationResult;
import se.curity.identityserver.sdk.authentication.AuthenticatorRequestHandler;
import se.curity.identityserver.sdk.http.HttpStatus;
import se.curity.identityserver.sdk.web.Request;
import se.curity.identityserver.sdk.web.Response;
import se.curity.identityserver.sdk.web.ResponseModel;

import javax.annotation.Nullable;
import java.util.Optional;

import static io.curity.authenticator.netid.PollingAuthenticatorConstants.EndUserMessageKeys.start_app;
import static io.curity.authenticator.netid.PollingAuthenticatorConstants.FormValueNames.CANCEL_URL;
import static io.curity.authenticator.netid.PollingAuthenticatorConstants.FormValueNames.FAILURE_URL;
import static io.curity.authenticator.netid.PollingAuthenticatorConstants.FormValueNames.POLL_URL;
import static io.curity.authenticator.netid.PollingAuthenticatorConstants.FormValueNames.RESTART_URL;
import static io.curity.authenticator.netid.PollingAuthenticatorConstants.FormValueNames.SERVICE_MESSAGE;
import static io.curity.authenticator.netid.utils.SdkConstants.ACTION;
import static io.curity.authenticator.netid.config.PluginComposer.getPollerPaths;
import static io.curity.authenticator.netid.config.PluginComposer.getStatusCodeMapping;

public final class WaitRequestHandler implements AuthenticatorRequestHandler<WaitRequestModel>
{
    private final NetIdAccessConfig _config;
    private final AuthenticatedState _authenticatedState;
    private final NetIdAccessClient _netIdAccessClient;
    private WebServicePoller _webservicePoller;
    private PollerPaths _pollerPaths;

    public WaitRequestHandler(NetIdAccessConfig configuration, AuthenticatedState authenticatedState, NetIdAccessServerSoapClient soapClient)
    {
        _config = configuration;
        _authenticatedState = authenticatedState;
        _netIdAccessClient = new NetIdAccessClient(configuration, soapClient);
    }

    @Override
    public WaitRequestModel preProcess(Request request, Response response)
    {
        _pollerPaths = getPollerPaths(request);

        if (request.isPostRequest())
        {
            _webservicePoller = new WebServicePoller(
                    _netIdAccessClient,
                    _pollerPaths,
                    _config.getSessionManager(),
                    _config.getAuthenticatorInformationProvider(),
                    _config.getExceptionFactory(),
                    _authenticatedState,
                    getStatusCodeMapping(request)
            );
        }

        response.setResponseModel(ResponseModel.templateResponseModel(ImmutableMap.of(),
                "wait/index"), Response.ResponseModelScope.NOT_FAILURE);

        return new WaitRequestModel(request);
    }

    @Override
    public Optional<AuthenticationResult> get(WaitRequestModel requestModel, Response response)
    {
        var authenticationUri = _config.getAuthenticatorInformationProvider().getFullyQualifiedAuthenticationUri();
        ImmutableMap.Builder<String, Object> map = ImmutableMap.<String, Object>builder()
                .put(SERVICE_MESSAGE, start_app)
                .put(ACTION, authenticationUri.getPath() + "/" + _pollerPaths.getPollerPath() + "?" + authenticationUri.getQuery())
                .put(RESTART_URL, authenticationUri.getPath())
                .put(CANCEL_URL, authenticationUri + "/" + _pollerPaths.getCancelPath())
                .put(FAILURE_URL, authenticationUri + "/" + _pollerPaths.getFailedPath())
                .put(POLL_URL, authenticationUri + "/" + _pollerPaths.getPollerPath());
        response.setResponseModel(new WaitResponseModel(map.build()), HttpStatus.OK);

        return Optional.empty();
    }

    @Override
    public Optional<AuthenticationResult> post(WaitRequestModel requestModel, Response response)
    {
        @Nullable AuthenticationResult result = _webservicePoller.getAuthenticationResult(
                requestModel.getPostRequestModel().isPollingDone(), response);
        return Optional.ofNullable(result);
    }
}
