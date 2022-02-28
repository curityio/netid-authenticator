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

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.html.HtmlEscapers;
import io.curity.authenticator.netid.common.PollingAuthenticatorConstants;
import io.curity.authenticator.netid.common.client.AuthenticateResponse;
import io.curity.authenticator.netid.common.client.PollingClient;
import io.curity.authenticator.netid.common.client.PollingClientAuthenticateException;
import io.curity.authenticator.netid.common.client.PollingClientException;
import io.curity.authenticator.netid.common.client.UnknownUserNameException;
import io.curity.authenticator.netid.common.model.PollerPaths;
import io.curity.authenticator.netid.common.model.UserNameGetModel;
import io.curity.authenticator.netid.common.model.UserNamePostModel;
import io.curity.authenticator.netid.common.model.UserNameRequestModel;
import io.curity.authenticator.netid.common.utils.NullUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.curity.identityserver.sdk.Nullable;
import se.curity.identityserver.sdk.attribute.Attribute;
import se.curity.identityserver.sdk.attribute.AttributeName;
import se.curity.identityserver.sdk.authentication.AuthenticatedState;
import se.curity.identityserver.sdk.authentication.AuthenticationResult;
import se.curity.identityserver.sdk.errors.ErrorCode;
import se.curity.identityserver.sdk.http.HttpStatus;
import se.curity.identityserver.sdk.service.ExceptionFactory;
import se.curity.identityserver.sdk.service.SessionManager;
import se.curity.identityserver.sdk.service.UserPreferenceManager;
import se.curity.identityserver.sdk.service.authentication.AuthenticatorInformationProvider;
import se.curity.identityserver.sdk.web.Request;
import se.curity.identityserver.sdk.web.Response;

import java.time.Instant;
import java.util.Optional;

import static io.curity.authenticator.netid.common.PollingAuthenticatorConstants.SessionKeys.AUTHENTICATION_STATE;
import static io.curity.authenticator.netid.common.PollingAuthenticatorConstants.SessionKeys.AUTOSTART_TOKEN;
import static io.curity.authenticator.netid.common.PollingAuthenticatorConstants.SessionKeys.INIT_TIME;
import static io.curity.authenticator.netid.common.PollingAuthenticatorConstants.SessionKeys.ORDER_REF;
import static io.curity.authenticator.netid.common.PollingAuthenticatorConstants.SessionKeys.QR_START_SECRET;
import static io.curity.authenticator.netid.common.PollingAuthenticatorConstants.SessionKeys.QR_START_TOKEN;
import static io.curity.authenticator.netid.common.PollingAuthenticatorConstants.SessionKeys.RESULT_ATTRIBUTES;
import static io.curity.authenticator.netid.common.PollingAuthenticatorConstants.SessionKeys.RESULT_SUBJECT;
import static io.curity.authenticator.netid.common.PollingAuthenticatorConstants.SessionKeys.USE_SAME_DEVICE;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static se.curity.identityserver.sdk.web.Response.ResponseModelScope.ANY;
import static se.curity.identityserver.sdk.web.ResponseModel.mapResponseModel;

public final class PollingAuthenticatorEnterUserNameLogic
{
    private static final Logger _logger = LoggerFactory.getLogger(PollingAuthenticatorEnterUserNameLogic.class);

    private final AuthenticatorInformationProvider _informationProvider;
    private final SessionManager _sessionManager;
    private final ExceptionFactory _exceptionFactory;
    private final UserPreferenceManager _userPreferenceManager;
    private final PollingClient _pollingClient;
    private final PollerPaths _pollerPaths;
    private final ErrorReportingStrategy _errorReportingStrategy;
    private final AuthenticatedState _authenticatedState;

    public PollingAuthenticatorEnterUserNameLogic(AuthenticatorInformationProvider informationProvider,
                                                  SessionManager sessionManager,
                                                  ExceptionFactory exceptionFactory,
                                                  UserPreferenceManager userPreferenceManager,
                                                  PollingClient pollingClient,
                                                  PollerPaths pollerPaths,
                                                  ErrorReportingStrategy errorReportingStrategy,
                                                  AuthenticatedState authenticatedState)
    {
        _informationProvider = informationProvider;
        _sessionManager = sessionManager;
        _exceptionFactory = exceptionFactory;
        _userPreferenceManager = userPreferenceManager;
        _pollingClient = pollingClient;
        _pollerPaths = pollerPaths;
        _errorReportingStrategy = errorReportingStrategy;
        _authenticatedState = authenticatedState;
    }

    public Optional<AuthenticationResult> get(UserNameRequestModel requestModel, Response response)
    {
        return get(requestModel, response, false);
    }

    public Optional<AuthenticationResult> get(UserNameRequestModel requestModel, Response response, boolean useQrCode)
    {
        @Nullable UserNameGetModel model = requestModel.getGetRequestModel();
        if (model == null)
        {
            // Programmer error, should not be null in get
            throw _exceptionFactory.internalServerException(ErrorCode.INVALID_SERVER_STATE, "Request model was null");
        }

        @Nullable String username = model.getAuthenticatedUsername();

        if (useQrCode)
        {
            // The QR code flow uses the same device flow, but shows a QR to be able to trigger it on another device
            proceedToAuth(username, true);
        }

        if (isNotEmpty(username))
        {
            // We should hide the username field in case the user chose "other device flow"
            response.putViewData("_knownUserName", true, ANY);
        }
        else
        {
            @Nullable String usernameFromCookie = _userPreferenceManager.getUsername();

            if (usernameFromCookie != null)
            {
                username = usernameFromCookie;
            }
        }

        if (username != null)
        {
            response.setResponseModel(mapResponseModel(ImmutableMap.of(
                    "_username", HtmlEscapers.htmlEscaper().escape(username))
            ), HttpStatus.OK);
        }

        return Optional.empty();
    }

    public Optional<AuthenticationResult> post(UserNameRequestModel requestModel, Response response)
    {
        @Nullable UserNamePostModel model = requestModel.getPostRequestModel();

        @Nullable String userName;

        if (model == null)
        {
            _logger.debug("Error, POST request model was empty");

            throw _exceptionFactory.badRequestException(ErrorCode.INVALID_INPUT);
        }
        else
        {
            userName = model.getUserName();
        }

        if (userName != null)
        {
            _userPreferenceManager.saveUsername(userName);
        }

        return proceedToAuth(userName, model.useSameDevice());
    }

    private Optional<AuthenticationResult> proceedToAuth(@Nullable String userName, boolean useSameDevice)
    {
        throw redirectToStartAppPage(useSameDevice, userName);
    }

    public UserPreferenceManager getUserPreferenceManager()
    {
        return _userPreferenceManager;
    }

    private RuntimeException redirectToStartAppPage(boolean useSameDevice, @Nullable String personalNumber)
    {
        AuthenticateResponse authenticateResponse = authenticate(personalNumber, useSameDevice);

        // Save info in session and clear old data
        _sessionManager.put(Attribute.of(
                AttributeName.of(AUTOSTART_TOKEN),
                authenticateResponse.getAutoStartToken()
        ));
        _sessionManager.put(Attribute.of(
                AttributeName.of(ORDER_REF),
                authenticateResponse.getTransactionId()
        ));
        _sessionManager.put(Attribute.of(
                AttributeName.of(USE_SAME_DEVICE), useSameDevice
        ));
        NullUtils.ifNotNull(authenticateResponse.getQrStartToken(), it ->
                _sessionManager.put(Attribute.of(AttributeName.of(QR_START_TOKEN), it)));
        NullUtils.ifNotNull(authenticateResponse.getQrStartSecret(), it ->
                _sessionManager.put(Attribute.of(AttributeName.of(QR_START_SECRET), it)));
        _sessionManager.put(Attribute.of(
                AttributeName.of(INIT_TIME),
                Instant.now().getEpochSecond()
        ));
        _sessionManager.remove(AUTHENTICATION_STATE);
        _sessionManager.remove(RESULT_SUBJECT);
        _sessionManager.remove(RESULT_ATTRIBUTES);
        _sessionManager.remove(PollingAuthenticatorConstants.SessionKeys.ERROR_MESSAGE);

        if (useSameDevice)
        {
            return redirectToLauncherPage();
        }
        else
        {
            return redirectToPoller();
        }
    }

    private AuthenticateResponse authenticate(@Nullable String personalNumber, boolean sameDeviceFlow)
    {
        String serviceName = _pollingClient.getServiceName();
        try
        {
            return _pollingClient.authenticate(personalNumber, sameDeviceFlow);
        }
        catch (PollingClientAuthenticateException e)
        {
            switch (e.getStatus())
            {
                case UNKNOWN_USER:
                    _logger.debug("Call to {} service failed: Unknown user id", serviceName);
                    throw new UnknownUserNameException(e.getStatus());
                case ALREADY_IN_PROGRESS:
                    _logger.trace("Call to {} service failed as authentication is already in progress",
                            serviceName);
                default:
                    _logger.debug("Call to {} service failed with status: {}", serviceName,
                            Throwables.getRootCause(e).getMessage());
                    throw reportFailure(e.getStatus().getMessageId());
            }
        }
        catch (PollingClientException e)
        {
            _logger.error("Call to {} service produced an unexpected error", serviceName, e);

            throw reportFailure(PollingAuthenticatorConstants.EndUserMessageKeys.INTERNAL_ERROR);
        }
    }

    public void onRequestModelValidationFailure(Request request, Response response,
                                                UserNamePostModel model)
    {
        if (request.isPostRequest())
        {
            response.setResponseModel(mapResponseModel(ImmutableMap.of(
                            "_postBack", model.dataOnError())),
                    HttpStatus.BAD_REQUEST);
        }
    }

    private RuntimeException redirectToLauncherPage()
    {
        var url = _informationProvider.getFullyQualifiedAuthenticationUri() + "/" + _pollerPaths.getLauncherPath();

        _logger.trace("redirecting to {}", url);

        return _exceptionFactory.redirectException(url);
    }

    private RuntimeException redirectToPoller()
    {
        var url = _informationProvider.getFullyQualifiedAuthenticationUri() + "/" + _pollerPaths.getPollerPath();

        _logger.trace("redirecting to {}", url);

        return _exceptionFactory.redirectException(url);
    }

    private RuntimeException reportFailure(String messageId)
    {
        return _errorReportingStrategy.getFailureException(messageId);
    }
}
