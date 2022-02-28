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

import io.curity.authenticator.netid.common.PollingAuthenticatorConstants;
import io.curity.authenticator.netid.common.model.PollerPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.curity.identityserver.sdk.attribute.Attribute;
import se.curity.identityserver.sdk.attribute.AttributeName;
import se.curity.identityserver.sdk.errors.ErrorCode;
import se.curity.identityserver.sdk.service.ExceptionFactory;
import se.curity.identityserver.sdk.service.SessionManager;
import se.curity.identityserver.sdk.service.authentication.AuthenticatorInformationProvider;

public final class ErrorReportingStrategy
{
    private static final Logger _logger = LoggerFactory.getLogger(ErrorReportingStrategy.class);

    private final AuthenticatorInformationProvider _informationProvider;
    private final SessionManager _sessionManager;
    private final ExceptionFactory _exceptionFactory;
    private final PollerPaths _pollerPaths;

    public ErrorReportingStrategy(AuthenticatorInformationProvider informationProvider,
                                  SessionManager sessionManager,
                                  ExceptionFactory exceptionFactory,
                                  PollerPaths pollerPaths)
    {
        _informationProvider = informationProvider;
        _sessionManager = sessionManager;
        _exceptionFactory = exceptionFactory;
        _pollerPaths = pollerPaths;
    }

    public RuntimeException getUserCancellationException(String errorMessage)
    {
        // When the user cancels the polling, always redirect to the failed path, regardless of the failure mode
        return redirectToFailedPath(errorMessage);
    }

    public RuntimeException getFailureException(String errorMessage)
    {
        switch (_pollerPaths.getFailureMode())
        {
            case REDIRECT_CLIENT:
                return redirectToFailedPath(errorMessage);
            case PROBLEM_JSON:
                return _exceptionFactory.badRequestException(ErrorCode.GENERIC_ERROR, errorMessage);
            default:
                _logger.warn("FailureStrategy case not covered: {}", _pollerPaths.getFailureMode());
                return _exceptionFactory.internalServerException(ErrorCode.INVALID_SERVER_STATE);
        }
    }

    private RuntimeException redirectToFailedPath(String errorMessage)
    {
        var url =  _informationProvider.getFullyQualifiedAuthenticationUri() + "/" + _pollerPaths.getFailedPath();

        _sessionManager.put(Attribute.of(
                AttributeName.of(PollingAuthenticatorConstants.SessionKeys.ERROR_MESSAGE),
                errorMessage));

        _logger.trace("redirecting to {}", url);
        return _exceptionFactory.redirectException(url);
    }
}
