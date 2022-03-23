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

import io.curity.authenticator.netid.ErrorReportingStrategy;
import io.curity.authenticator.netid.config.NetIdAccessConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.curity.identityserver.sdk.authentication.AuthenticationResult;
import se.curity.identityserver.sdk.authentication.AuthenticatorRequestHandler;
import se.curity.identityserver.sdk.service.ExceptionFactory;
import se.curity.identityserver.sdk.service.SessionManager;
import se.curity.identityserver.sdk.service.authentication.AuthenticatorInformationProvider;
import se.curity.identityserver.sdk.web.Request;
import se.curity.identityserver.sdk.web.Response;

import java.util.Optional;

import static io.curity.authenticator.netid.config.PluginComposer.getPollerPaths;

public final class CancelRequestHandler implements AuthenticatorRequestHandler<Request>
{
    private static final Logger _logger = LoggerFactory.getLogger(CancelRequestHandler.class);

    private final ExceptionFactory _exceptionFactory;
    private final SessionManager _sessionManager;
    private final AuthenticatorInformationProvider _informationProvider;

    public CancelRequestHandler(ExceptionFactory exceptionFactory, NetIdAccessConfig configuration)
    {
        _exceptionFactory = exceptionFactory;
        _sessionManager = configuration.getSessionManager();
        _informationProvider = configuration.getAuthenticatorInformationProvider();
    }

    @Override
    public Request preProcess(Request request, Response response)
    {
        return request;
    }

    @Override
    public Optional<AuthenticationResult> get(Request request, Response response)
    {
        // The current transaction will time out after a while, and there is no way of cancelling without creating a new one.
        // Net iD does not respond with ALREADY_IN_PROGRESS if a transaction is already started.
        // Instead, the previous one is cancelled and a new transaction is started. No need to cancel anything here.
        _logger.debug("Reporting to the user that the transaction is cancelled, but no action is being taken as NetID" +
                " does not provide an explicit way to cancel a transaction, it will time out after a short time");

        var pollerPaths = getPollerPaths(request);
        var errorReportingStrategy = new ErrorReportingStrategy(
                _informationProvider,
                _sessionManager,
                _exceptionFactory,
                pollerPaths
        );

        throw errorReportingStrategy.getUserCancellationException("error.user-cancelled");
    }

    @Override
    public Optional<AuthenticationResult> post(Request request, Response response)
    {
        throw _exceptionFactory.methodNotAllowed();
    }
}
