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

package io.curity.authenticator.netid.common.logic;

import com.google.common.collect.ImmutableMap;
import io.curity.authenticator.netid.common.client.WebServicePoller;
import io.curity.authenticator.netid.common.model.PollerPaths;
import io.curity.authenticator.netid.common.model.WaitRequestModel;
import io.curity.authenticator.netid.common.model.WaitResponseModel;
import io.curity.authenticator.netid.common.utils.NullUtils;
import se.curity.identityserver.sdk.authentication.AuthenticationResult;
import se.curity.identityserver.sdk.http.HttpStatus;
import se.curity.identityserver.sdk.service.authentication.AuthenticatorInformationProvider;
import se.curity.identityserver.sdk.web.Request;
import se.curity.identityserver.sdk.web.Response;
import se.curity.identityserver.sdk.web.ResponseModel;

import javax.annotation.Nullable;
import java.util.Optional;

import static io.curity.authenticator.netid.common.PollingAuthenticatorConstants.EndUserMessageKeys.START_APP;
import static io.curity.authenticator.netid.common.PollingAuthenticatorConstants.FormValueNames.CANCEL_URL;
import static io.curity.authenticator.netid.common.PollingAuthenticatorConstants.FormValueNames.FAILURE_URL;
import static io.curity.authenticator.netid.common.PollingAuthenticatorConstants.FormValueNames.POLL_URL;
import static io.curity.authenticator.netid.common.PollingAuthenticatorConstants.FormValueNames.QR_CODE;
import static io.curity.authenticator.netid.common.PollingAuthenticatorConstants.FormValueNames.RESTART_URL;
import static io.curity.authenticator.netid.common.PollingAuthenticatorConstants.FormValueNames.SERVICE_MESSAGE;
import static io.curity.authenticator.netid.common.utils.SdkConstants.ACTION;

public class WaitRequestLogic
{
    private final AuthenticatorInformationProvider _informationProvider;
    private final WebServicePoller _webservicePoller;
    private final PollerPaths _pollerPaths;

    public WaitRequestLogic(AuthenticatorInformationProvider informationProvider,
                            WebServicePoller webservicePoller,
                            PollerPaths pollerPaths)
    {
        _informationProvider = informationProvider;
        _webservicePoller = webservicePoller;
        _pollerPaths = pollerPaths;
    }

    public WaitRequestModel preProcess(Request request, Response response)
    {
        response.setResponseModel(ResponseModel.templateResponseModel(ImmutableMap.of(),
                "wait/index"), Response.ResponseModelScope.NOT_FAILURE);

        return new WaitRequestModel(request);
    }

    public Optional<AuthenticationResult> get(Response response, @Nullable String qrCode)
    {
        var authenticationUri = _informationProvider.getFullyQualifiedAuthenticationUri();
        ImmutableMap.Builder<String, Object> map = ImmutableMap.<String, Object>builder()
                .put(SERVICE_MESSAGE, START_APP)
                .put(ACTION, authenticationUri.getPath() + "/" + _pollerPaths.getPollerPath() + "?" + authenticationUri.getQuery())
                .put(RESTART_URL, authenticationUri.getPath())
                .put(CANCEL_URL, authenticationUri + "/" + _pollerPaths.getCancelPath())
                .put(FAILURE_URL, authenticationUri + "/" + _pollerPaths.getFailedPath())
                .put(POLL_URL, authenticationUri + "/" + _pollerPaths.getPollerPath());
        NullUtils.ifNotNull(qrCode, it -> map.put(QR_CODE, it));
        response.setResponseModel(new WaitResponseModel(map.build()), HttpStatus.OK);

        return Optional.empty();
    }

    public Optional<AuthenticationResult> post(boolean isPollingDone, Response response, @Nullable String qrCode)
    {
        @Nullable AuthenticationResult result = _webservicePoller.getAuthenticationResult(
                isPollingDone, response, qrCode);
        return Optional.ofNullable(result);
    }
}
