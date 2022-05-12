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

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.html.HtmlEscapers;
import io.curity.authenticator.netid.NetIdAccessServerSoapClient;
import io.curity.authenticator.netid.client.NetIdAccessClient;
import io.curity.authenticator.netid.PollingAuthenticatorConstants;
import io.curity.authenticator.netid.client.AuthenticateResponse;
import io.curity.authenticator.netid.client.PollingClient;
import io.curity.authenticator.netid.client.PollingClientAuthenticateException;
import io.curity.authenticator.netid.client.PollingClientException;
import io.curity.authenticator.netid.client.UnknownUserNameException;
import io.curity.authenticator.netid.ErrorReportingStrategy;
import io.curity.authenticator.netid.model.PollerPaths;
import io.curity.authenticator.netid.model.UserNameGetModel;
import io.curity.authenticator.netid.model.UserNamePostModel;
import io.curity.authenticator.netid.config.NetIdAccessConfig;
import io.curity.authenticator.netid.model.NonValidatingUserNameRequestModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.curity.identityserver.sdk.Nullable;
import se.curity.identityserver.sdk.attribute.Attribute;
import se.curity.identityserver.sdk.attribute.AttributeName;
import se.curity.identityserver.sdk.authentication.AuthenticatedState;
import se.curity.identityserver.sdk.authentication.AuthenticationResult;
import se.curity.identityserver.sdk.authentication.AuthenticatorRequestHandler;
import se.curity.identityserver.sdk.errors.ErrorCode;
import se.curity.identityserver.sdk.http.HttpStatus;
import se.curity.identityserver.sdk.service.ExceptionFactory;
import se.curity.identityserver.sdk.service.SessionManager;
import se.curity.identityserver.sdk.service.UserPreferenceManager;
import se.curity.identityserver.sdk.service.authentication.AuthenticatorInformationProvider;
import se.curity.identityserver.sdk.web.Request;
import se.curity.identityserver.sdk.web.Response;
import se.curity.identityserver.sdk.web.alerts.ErrorMessage;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static io.curity.authenticator.netid.PollingAuthenticatorConstants.SessionKeys.AUTHENTICATION_STATE;
import static io.curity.authenticator.netid.PollingAuthenticatorConstants.SessionKeys.AUTOSTART_TOKEN;
import static io.curity.authenticator.netid.PollingAuthenticatorConstants.SessionKeys.INIT_TIME;
import static io.curity.authenticator.netid.PollingAuthenticatorConstants.SessionKeys.ORDER_REF;
import static io.curity.authenticator.netid.PollingAuthenticatorConstants.SessionKeys.RESULT_ATTRIBUTES;
import static io.curity.authenticator.netid.PollingAuthenticatorConstants.SessionKeys.RESULT_SUBJECT;
import static io.curity.authenticator.netid.PollingAuthenticatorConstants.SessionKeys.USE_SAME_DEVICE;
import static io.curity.authenticator.netid.config.PluginComposer.getPollerPaths;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static se.curity.identityserver.sdk.web.Response.ResponseModelScope.ANY;
import static se.curity.identityserver.sdk.web.ResponseModel.mapResponseModel;
import static se.curity.identityserver.sdk.web.ResponseModel.templateResponseModel;

public final class EnterUserNameRequestHandler
        implements AuthenticatorRequestHandler<NonValidatingUserNameRequestModel>
{
    private final AuthenticatedState _authenticatedState;
    private final ExceptionFactory _exceptionFactory;
    private final UserPreferenceManager _userPreferenceManager;
    private final AuthenticatorInformationProvider _informationProvider;
    private final SessionManager _sessionManager;
    private final PollingClient _netIdAccessClient;
    private PollerPaths _pollerPaths;
    private ErrorReportingStrategy _errorReportingStrategy;

    private static final Logger _logger = LoggerFactory.getLogger(EnterUserNameRequestHandler.class);

    public EnterUserNameRequestHandler(AuthenticatedState authenticatedState, NetIdAccessConfig configuration, NetIdAccessServerSoapClient soapClient)
    {
        _authenticatedState = authenticatedState;
        _exceptionFactory = configuration.getExceptionFactory();
        _informationProvider = configuration.getAuthenticatorInformationProvider();
        _sessionManager = configuration.getSessionManager();
        _userPreferenceManager = configuration.getUserPreferenceManager();
        _netIdAccessClient = new NetIdAccessClient(configuration, soapClient);
    }

    @Override
    public NonValidatingUserNameRequestModel preProcess(Request request, Response response)
    {
        setResponseModel(request, response);

        _pollerPaths = getPollerPaths(request);
        _errorReportingStrategy = new ErrorReportingStrategy(
                _informationProvider,
                _sessionManager,
                _exceptionFactory,
                _pollerPaths
        );


        return createRequestModel(request, _userPreferenceManager, _authenticatedState);
    }

    @Override
    public Optional<AuthenticationResult> get(NonValidatingUserNameRequestModel requestModel, Response response)
    {
        @Nullable UserNameGetModel model = requestModel.getGetRequestModel();
        if (model == null)
        {
            // Programmer error, should not be null in get
            throw _exceptionFactory.internalServerException(ErrorCode.INVALID_SERVER_STATE, "Request model was null");
        }

        @Nullable String username = model.getAuthenticatedUsername();

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

    @Override
    public Optional<AuthenticationResult> post(NonValidatingUserNameRequestModel requestModel, Response response)
    {
        try
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
        catch (UnknownUserNameException e)
        {
            // Stay on the same page to let the user give a new ID
            @Nullable UserNamePostModel postRequestModel = requestModel.getPostRequestModel();
            if (postRequestModel != null)
            {
                response.setResponseModel(mapResponseModel(postRequestModel.dataOnError()),
                        Response.ResponseModelScope.FAILURE);
            }
            throw _exceptionFactory.badRequestException(ErrorCode.INVALID_INPUT, "username.invalid");
        }
    }

    @Override
    public void onRequestModelValidationFailure(Request request, Response response, Set<ErrorMessage> errorMessages)
    {
        NonValidatingUserNameRequestModel model = createRequestModel(request, _userPreferenceManager, _authenticatedState);
        if (model.getPostRequestModel() != null)
        {
            if (request.isPostRequest())
            {
                response.setResponseModel(mapResponseModel(ImmutableMap.of(
                                "_postBack", model.getPostRequestModel().dataOnError())),
                        HttpStatus.BAD_REQUEST);
            }
        }
    }

    private static NonValidatingUserNameRequestModel createRequestModel(Request request,
                                                                        UserPreferenceManager userPreferenceManager,
                                                                        AuthenticatedState authenticatedState)
    {
        return new NonValidatingUserNameRequestModel(request, authenticatedState, userPreferenceManager);
    }

    private void setResponseModel(Request request, Response response)
    {
        if (request.isGetRequest())
        {
            response.setResponseModel(templateResponseModel(ImmutableMap.of(),
                            "enter-username/index"),
                    Response.ResponseModelScope.NOT_FAILURE);
        }
        else if (request.isPostRequest())
        {
            response.setResponseModel(templateResponseModel(ImmutableMap.of(),
                            "enter-username/index"),
                    Response.ResponseModelScope.NOT_FAILURE);

            response.setResponseModel(templateResponseModel(ImmutableMap.of(),
                            "enter-username/index"),
                    HttpStatus.BAD_REQUEST);
        }
    }

    private Optional<AuthenticationResult> proceedToAuth(@Nullable String userName, boolean useSameDevice)
    {
        throw redirectToStartAppPage(useSameDevice, userName);
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
        String serviceName = _netIdAccessClient.getServiceName();
        try
        {
            return _netIdAccessClient.authenticate(personalNumber, sameDeviceFlow);
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
