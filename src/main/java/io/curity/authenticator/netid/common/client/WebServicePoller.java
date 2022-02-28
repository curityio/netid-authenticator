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

package io.curity.authenticator.netid.common.client;

import io.curity.authenticator.netid.common.GenericError;
import io.curity.authenticator.netid.common.PollingAuthenticatorConstants;
import io.curity.authenticator.netid.common.model.PollerPaths;
import io.curity.authenticator.netid.common.model.PollingResult;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.curity.identityserver.sdk.attribute.Attribute;
import se.curity.identityserver.sdk.attribute.AttributeName;
import se.curity.identityserver.sdk.attribute.AttributeValue;
import se.curity.identityserver.sdk.attribute.AuthenticationAttributes;
import se.curity.identityserver.sdk.authentication.AuthenticatedState;
import se.curity.identityserver.sdk.authentication.AuthenticationResult;
import se.curity.identityserver.sdk.errors.ErrorCode;
import se.curity.identityserver.sdk.http.HttpStatus;
import se.curity.identityserver.sdk.service.ExceptionFactory;
import se.curity.identityserver.sdk.service.SessionManager;
import se.curity.identityserver.sdk.service.authentication.AuthenticatorInformationProvider;
import se.curity.identityserver.sdk.web.Response;

import javax.annotation.Nullable;

import java.util.Optional;

import static io.curity.authenticator.netid.common.PollingAuthenticatorConstants.EndUserMessageKeys.GENERAL_ERROR;
import static io.curity.authenticator.netid.common.PollingAuthenticatorConstants.FormValueNames.RESTART_URL;
import static io.curity.authenticator.netid.common.PollingAuthenticatorConstants.SessionKeys.AUTHENTICATION_STATE;
import static io.curity.authenticator.netid.common.PollingAuthenticatorConstants.SessionKeys.ERROR_MESSAGE;
import static io.curity.authenticator.netid.common.PollingAuthenticatorConstants.SessionKeys.ORDER_REF;
import static io.curity.authenticator.netid.common.PollingAuthenticatorConstants.SessionKeys.RESULT_ATTRIBUTES;
import static io.curity.authenticator.netid.common.PollingAuthenticatorConstants.SessionKeys.RESULT_SUBJECT;
import static io.curity.authenticator.netid.common.PollingAuthenticatorConstants.SessionKeys.USE_SAME_DEVICE;
import static org.apache.commons.lang3.StringUtils.isEmpty;

public class WebServicePoller
{

    private static final Logger _logger = LoggerFactory.getLogger(WebServicePoller.class);

    private final PollingClient _pollingClient;
    private final PollerPaths _pollerPaths;
    private final SessionManager _sessionManager;
    private final AuthenticatorInformationProvider _informationProvider;
    private final AuthenticatedState _authenticatedState;
    private final ExceptionFactory _exceptionFactory;
    private final StatusCodeMapping _statusCodeMapping;

    public WebServicePoller(PollingClient pollingClient,
                            PollerPaths pollerPaths,
                            SessionManager sessionManager,
                            AuthenticatorInformationProvider informationProvider,
                            ExceptionFactory exceptionFactory,
                            AuthenticatedState authenticatedState,
                            StatusCodeMapping statusCodeMapping)
    {
        _pollingClient = pollingClient;
        _pollerPaths = pollerPaths;
        _sessionManager = sessionManager;
        _informationProvider = informationProvider;
        _authenticatedState = authenticatedState;
        _exceptionFactory = exceptionFactory;
        _statusCodeMapping = statusCodeMapping;
    }

    private void poll(Response response, String transactionId, boolean useSameDevice, @Nullable String qrCode)
    {
        _logger.trace("Polling for authentication status for transaction ID/OrderRef {}", transactionId);

        CollectResponse collectResponse;
        try
        {
            collectResponse = _pollingClient.poll(transactionId);
        }
        catch (PollingClientCollectException e)
        {
            _logger.debug("Polling failed with status {}", e.getStatus());

            pollFailed(response, e.getStatus()); // Sets status to 201

            return;
        }
        catch (PollingClientException e)
        {
            _logger.debug("Polling failed with unexpected error", e);

            pollFailed(response, null); // Sets status to 201

            return;
        }

        _logger.trace("Polling status from server: {}", collectResponse.getStatus());

        if (collectResponse.getStatus() == CollectStatus.COMPLETE)
        {
            _logger.trace("Indicating to poller that authentication has completed");

            // TODO: Verify result, check not-before, after etc?

            //Tell the poller we're ready
            pollSuccess(response); // Sets status to 202 (depending on mapping)

            // Store identity in Session
            _sessionManager.put(Attribute.of(RESULT_ATTRIBUTES, AttributeValue.of(collectResponse.getAuthenticationAttributes(collectResponse.getSubject()))));
            _sessionManager.put(Attribute.of(RESULT_SUBJECT, collectResponse.getSubject()));
            // Set a flag that we got done flag from server
            _sessionManager.put(Attribute.of(AUTHENTICATION_STATE, true));

            // Still in progress
            return;
        }

        String messageId = useSameDevice
                ? collectResponse.getStatus().getSameDeviceMessageId()
                : collectResponse.getStatus().getOtherDeviceMessageId();

        _logger.debug("Mapped collect response status {} to message ID {}{} using same device",
                collectResponse.getStatus(), messageId, useSameDevice ? "" : " not");

        HttpStatus httpStatus = _statusCodeMapping.keepPolling();
        var pollUrl = _informationProvider.getFullyQualifiedAuthenticationUri() + "/" + _pollerPaths.getPollerPath();
        var cancelUrl = _informationProvider.getFullyQualifiedAuthenticationUri() + "/" + _pollerPaths.getCancelPath();
        response.setResponseModel(new PollingResult.Pending(messageId, pollUrl, cancelUrl, qrCode), httpStatus);
        response.setHttpStatus(httpStatus);
    }

    private void pollFailed(Response response, @Nullable CollectFaultStatus collectFaultStatus)
    {
        String messageId = collectFaultStatus == null
                ? GENERAL_ERROR
                : collectFaultStatus.getMessageId();

        _logger.trace("Saving error message: {}", messageId);

        _sessionManager.put(Attribute.of(AttributeName.of(ERROR_MESSAGE), messageId));

        var pollUrl = _informationProvider.getFullyQualifiedAuthenticationUri() + "/" + _pollerPaths.getPollerPath();
        var cancelUrl = _informationProvider.getFullyQualifiedAuthenticationUri() + "/" + _pollerPaths.getCancelPath();
        var failUrl = _informationProvider.getFullyQualifiedAuthenticationUri(
                // The poller error is the only resource which does not abide by ErrorReportingStrategy.
                // Instead, we only consider the StatusCodeMapping to define the HTTP status code.
                ) + "/" + _pollerPaths.getFailedPath();

        boolean fatalError = collectFaultStatus == null || collectFaultStatus.isFatal();
        HttpStatus httpStatus = _statusCodeMapping.pollingFailure(fatalError);
        response.setResponseModel(new PollingResult.Failed(failUrl, pollUrl, cancelUrl, messageId, collectFaultStatus), httpStatus);

        // this http status may be an error code... In case it is, we need to let general-purpose error templates
        // know about the reason of this failure, so we add the failure message to the FAILURE scope.
        response.putViewData("_systemErrorMessage",
                messageId, Response.ResponseModelScope.FAILURE);

        response.setHttpStatus(httpStatus);
    }

    private void pollSuccess(Response response)
    {
        HttpStatus httpStatus = _statusCodeMapping.pollingDone();
        String finishOffUrl = _informationProvider.getFullyQualifiedAuthenticationUri() + "/" + _pollerPaths.getPollerPath();
        response.setResponseModel(new PollingResult.Success(finishOffUrl), httpStatus);
        response.setHttpStatus(httpStatus);
    }

    /**
     * Poll for authentication result
     *
     * @param isPollingDone true if the client claims its finished
     * @param response      http response
     * @return result if the authentication was finished, null otherwise
     */
    public AuthenticationResult getAuthenticationResult(boolean isPollingDone, Response response)
    {
        return getAuthenticationResult(isPollingDone, response, null);
    }

    /**
     * Poll for authentication result
     *
     * @param isPollingDone true if the client claims its finished
     * @param response      http response
     * @param qrCode        qrCode if present
     * @return result if the authentication was finished, null otherwise
     */
    @Nullable
    public AuthenticationResult getAuthenticationResult(
            boolean isPollingDone, Response response, @Nullable String qrCode)
    {
        boolean authenticationComplete = Optional.ofNullable(_sessionManager.get(AUTHENTICATION_STATE))
                .map(attribute -> attribute.getOptionalValueOfType(Boolean.class))
                .orElse(false);
        var transactionId = Optional.ofNullable(_sessionManager.get(ORDER_REF))
                .map(attribute -> attribute.getOptionalValueOfType(String.class))
                .orElse("");
        boolean useSameDevice = Optional.ofNullable(_sessionManager.get(USE_SAME_DEVICE))
                .map(attribute -> attribute.getOptionalValueOfType(Boolean.class))
                .orElse(false);

        if (isEmpty(transactionId))
        {
            _logger.debug("Tried to poll without a transaction id.");
            throw _exceptionFactory.badRequestException(ErrorCode.MISSING_PARAMETERS);
        }
        else if (authenticationComplete)
        {
            _logger.debug("Getting authenticated user from state...");

            // Get Authentication Result from state


            @Nullable var sessionAttributes = _sessionManager.get(RESULT_ATTRIBUTES);
            @Nullable var sessionSubject = _sessionManager.get(RESULT_SUBJECT);

            if (sessionAttributes != null)
            {
                var subject = sessionSubject != null ? sessionSubject.getOptionalValueOfType(String.class) : null;
                return getAuthenticationResultWhenSuccess(_authenticatedState,
                        sessionAttributes.getOptionalValueOfType(AuthenticationAttributes.class),
                        subject);
            }
            else
            {
                _logger.info("The session ID was not found");

                throw _exceptionFactory.internalServerException(ErrorCode.INVALID_SERVER_STATE);
            }
        }
        else if (isPollingDone)
        {
            // The form was posted up to us without authentication being finished. This is probably an
            // attempt to hack us or some weird setup

            _logger.info("Authentication failed, and the form was posted. This could be an " +
                    "attempt to subvert the system or an odd development error, as there is no normal way the form " +
                    "should have been submitted.");

            response.putViewData(RESTART_URL, _informationProvider.getFullyQualifiedAuthenticationUri().getPath(),
                    Response.ResponseModelScope.ANY);
            response.addErrorMessage(new GenericError("error.authentication.failed"));
        }
        else
        {
            poll(response, transactionId, useSameDevice, qrCode);
        }

        return null;
    }

    private AuthenticationResult getAuthenticationResultWhenSuccess(AuthenticatedState authenticatedState,
                                                                    @Nullable AuthenticationAttributes authenticationAttributes,
                                                                    @Nullable String subject)
    {
        PollingAuthenticatorConstants.SessionKeys.all.forEach(_sessionManager::remove);

        if (authenticationAttributes == null)
        {
            _logger.debug("Could not find authenticated user in state, aborting.");

            throw _exceptionFactory.badRequestException(ErrorCode.INVALID_INPUT);
        }

        // We only use subject information from the collected response,
        // which has the personal number that was freshly authenticated,
        // and never what is in the authenticated state.
        if (subject != null)
        {
            if (_logger.isDebugEnabled())
            {
                if (authenticatedState.isAuthenticated() && !authenticatedState.getUsername().equals(subject))
                {
                    _logger.debug(
                            "BankID authenticated subject '{}' does not match authenticated state subject '{}'",
                            subject,
                            authenticatedState.getUsername()
                    );
                }
            }
        }
        else
        {
            _logger.warn("Could not find personal number in authentication service response");

            throw _exceptionFactory.internalServerException(ErrorCode.EXTERNAL_SERVICE_ERROR);
        }

        if (_logger.isDebugEnabled())
        {
            _logger.debug("User {} authenticated", StringUtils.replace(subject, "....$", "XXXX"));
        }

        return new AuthenticationResult(authenticationAttributes);
    }

    /**
     * Mapping between different polling results and HTTP status codes.
     * <p>
     * This interface allows different strategies to be used regarding HTTP status codes while polling.
     */
    public interface StatusCodeMapping
    {
        HttpStatus keepPolling();

        HttpStatus pollingFailure(boolean isFatalError);

        HttpStatus pollingDone();
    }
}

/**
 * Custom HTTP status codes that are understood by the Curity JavaScript Poller at
 * {@code se.curity.utils.poller.startPolling}.
 * <p>
 * These status codes do not follow known HTTP semantics, but as they have been used for a long time as of writing,
 * we may not change them without causing likely breakages on customers, which would benefit no one. So here we
 * "formalize" the custom scheme and have to just live with it.
 */
final class CustomPollerStatusCodes implements WebServicePoller.StatusCodeMapping
{
    static final CustomPollerStatusCodes INSTANCE = new CustomPollerStatusCodes();

    @Override
    public HttpStatus keepPolling()
    {
        return HttpStatus.CREATED;
    }

    @Override
    public HttpStatus pollingFailure(boolean isFatalError)
    {
        // legacy logic - the JS poller will "see" the failure redirect without concern for the status code
        return HttpStatus.CREATED;
    }

    @Override
    public HttpStatus pollingDone()
    {
        return HttpStatus.ACCEPTED;
    }
}

/**
 * A more HTTP-friendly status code mapping for the poller.
 * <p>
 * May be injected into the {@link WebServicePoller} to change status codes it reports.
 */
final class SemanticHttpPollerStatusCodeMapping implements WebServicePoller.StatusCodeMapping
{
    public static final SemanticHttpPollerStatusCodeMapping INSTANCE = new SemanticHttpPollerStatusCodeMapping();

    @Override
    public HttpStatus keepPolling()
    {
        return HttpStatus.OK;
    }

    @Override
    public HttpStatus pollingFailure(boolean isFatalError)
    {
        return isFatalError ? HttpStatus.BAD_REQUEST : HttpStatus.OK;
    }

    @Override
    public HttpStatus pollingDone()
    {
        return HttpStatus.OK;
    }
}
