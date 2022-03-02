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
import com.google.common.html.HtmlEscapers;
import io.curity.authenticator.netid.model.FailedRequestModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.curity.identityserver.sdk.authentication.AuthenticationResult;
import se.curity.identityserver.sdk.authentication.AuthenticatorRequestHandler;
import se.curity.identityserver.sdk.http.HttpStatus;
import se.curity.identityserver.sdk.service.ExceptionFactory;
import se.curity.identityserver.sdk.service.SessionManager;
import se.curity.identityserver.sdk.service.authentication.AuthenticatorInformationProvider;
import se.curity.identityserver.sdk.web.Request;
import se.curity.identityserver.sdk.web.Response;

import java.util.Optional;

import static io.curity.authenticator.netid.PollingAuthenticatorConstants.FormValueNames.RESTART_URL;
import static io.curity.authenticator.netid.PollingAuthenticatorConstants.FormValueNames.SERVICE_MESSAGE;
import static se.curity.identityserver.sdk.web.ResponseModel.mapResponseModel;
import static se.curity.identityserver.sdk.web.ResponseModel.templateResponseModel;

public final class FailedRequestHandler implements AuthenticatorRequestHandler<FailedRequestModel>
{
    private final Logger _logger = LoggerFactory.getLogger(FailedRequestHandler.class);

    private final AuthenticatorInformationProvider _informationProvider;
    private final ExceptionFactory _exceptionFactory;
    private final SessionManager _sessionManager;

    public FailedRequestHandler(ExceptionFactory exceptionFactory,
                                SessionManager sessionManager,
                                AuthenticatorInformationProvider informationProvider)
    {
        _logger.trace("FailedRequestHandler constructor");

        _exceptionFactory = exceptionFactory;
        _sessionManager = sessionManager;
        _informationProvider = informationProvider;
    }

    @Override
    public FailedRequestModel preProcess(Request request, Response response)
    {
        // Adding the failure template in non-failure cases because this request handler addresses cases
        // where BankID authentication failed. So, rendering the failure template is normal in this case.
        response.setResponseModel(templateResponseModel(ImmutableMap.of(),
                "failed/index"), Response.ResponseModelScope.NOT_FAILURE);

        return new FailedRequestModel(request, _sessionManager);
    }

    @Override
    public Optional<AuthenticationResult> get(FailedRequestModel requestModel, Response response)
    {
        _logger.trace("FailedRequestHandler.get");

        FailedRequestModel.Get model = requestModel.getGetRequestModel();

        response.setResponseModel(mapResponseModel(ImmutableMap.of(
                SERVICE_MESSAGE, HtmlEscapers.htmlEscaper().escape(model.getErrorMessage()),
                RESTART_URL, _informationProvider.getAuthenticationBaseUri().getPath()
        )), HttpStatus.OK);

        return Optional.empty();
    }

    @Override
    public Optional<AuthenticationResult> post(FailedRequestModel requestModel, Response response)
    {
        _logger.trace("FailedRequestHandler.post");

        throw _exceptionFactory.methodNotAllowed("Post not supported for this url.");
    }
}
