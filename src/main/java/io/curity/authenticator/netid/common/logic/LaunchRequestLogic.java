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
import io.curity.authenticator.netid.common.PollingAuthenticatorConstants;
import io.curity.authenticator.netid.common.client.WebServicePoller;
import io.curity.authenticator.netid.common.model.AuthenticationCompletedResponseModel;
import io.curity.authenticator.netid.common.model.LaunchRequestModel;
import io.curity.authenticator.netid.common.model.LaunchResponseModel;
import io.curity.authenticator.netid.common.model.PollerPaths;
import io.curity.authenticator.netid.common.model.PollingResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.curity.identityserver.sdk.attribute.Attribute;
import se.curity.identityserver.sdk.attribute.AttributeName;
import se.curity.identityserver.sdk.authentication.AuthenticationResult;
import se.curity.identityserver.sdk.http.HttpStatus;
import se.curity.identityserver.sdk.service.SessionManager;
import se.curity.identityserver.sdk.service.authentication.AuthenticatorInformationProvider;
import se.curity.identityserver.sdk.web.Request;
import se.curity.identityserver.sdk.web.Response;
import se.curity.identityserver.sdk.web.ResponseModel;

import javax.annotation.Nullable;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.function.Consumer;

import static io.curity.authenticator.netid.common.PollingAuthenticatorConstants.FormValueNames.AUTOSTART_TOKEN;
import static io.curity.authenticator.netid.common.PollingAuthenticatorConstants.FormValueNames.CANCEL_URL;
import static io.curity.authenticator.netid.common.PollingAuthenticatorConstants.FormValueNames.FAILURE_URL;
import static io.curity.authenticator.netid.common.PollingAuthenticatorConstants.FormValueNames.FORM_LAUNCH_COUNT;
import static io.curity.authenticator.netid.common.PollingAuthenticatorConstants.FormValueNames.POLL_URL;
import static io.curity.authenticator.netid.common.PollingAuthenticatorConstants.FormValueNames.RESTART_URL;
import static io.curity.authenticator.netid.common.PollingAuthenticatorConstants.FormValueNames.RETURN_TO_URL;
import static io.curity.authenticator.netid.common.PollingAuthenticatorConstants.SessionKeys.AUTHENTICATION_STATE;
import static io.curity.authenticator.netid.common.PollingAuthenticatorConstants.SessionKeys.SESSION_LAUNCH_COUNT;
import static io.curity.authenticator.netid.common.utils.SdkConstants.ACTION;
import static io.curity.authenticator.netid.common.utils.SdkConstants.CSP_OVERRIDE_CHILD_SRC;

public final class LaunchRequestLogic
{
    public static final class UriScheme
    {
        private final String _scheme;

        public UriScheme(String scheme)
        {
            _scheme = scheme;
        }
    }

    private static final Logger _logger = LoggerFactory.getLogger(LaunchRequestLogic.class);
    private final AuthenticatorInformationProvider _informationProvider;
    private final WebServicePoller _webservicePoller;
    private final PollerPaths _pollerPaths;
    protected final SessionManager _sessionManager;
    private final String _launchScheme;

    public LaunchRequestLogic(AuthenticatorInformationProvider informationProvider,
                              WebServicePoller webservicePoller,
                              SessionManager sessionManager,
                              UriScheme launchScheme,
                              PollerPaths pollerPaths)
    {
        _informationProvider = informationProvider;
        _webservicePoller = webservicePoller;
        _pollerPaths = pollerPaths;
        _sessionManager = sessionManager;
        _launchScheme = launchScheme._scheme;
    }

    public LaunchRequestModel preProcess(Request request, Response response)
    {
        response.setResponseModel(ResponseModel.templateResponseModel(ImmutableMap.of(),
                        "launch/index"),
                Response.ResponseModelScope.NOT_FAILURE);

        return new LaunchRequestModel(request, _sessionManager);
    }

    public Optional<AuthenticationResult> get(LaunchRequestModel requestModel, Response response)
    {
        return get(requestModel, response, it -> {});
    }

    public Optional<AuthenticationResult> get(LaunchRequestModel requestModel, Response response,
            Consumer<ImmutableMap.Builder<String, Object>> modelUpdate)
    {
        LaunchRequestModel.Get model = requestModel.getGetRequestModel();

        // Check if we're already done
        _webservicePoller.getAuthenticationResult(false, response);

        var authenticationUri = _informationProvider.getFullyQualifiedAuthenticationUri();

        boolean error = _sessionManager.get(PollingAuthenticatorConstants.SessionKeys.ERROR_MESSAGE) != null;

        if (!error)
        {
            response.setHttpStatus(HttpStatus.OK); // The poller set this to 201. Change back to 200.
        }

        boolean authenticationComplete = Optional
                .ofNullable(_sessionManager.get(AUTHENTICATION_STATE))
                .map(attribute -> attribute.getOptionalValueOfType(Boolean.class))
                .orElse(false);

        if (authenticationComplete)
        {
            response.setResponseModel(new AuthenticationCompletedResponseModel(
                    authenticationUri + "/" + _pollerPaths.getPollerPath(),
                    true
            ), HttpStatus.OK);

            response.setHttpStatus(HttpStatus.OK);

            return Optional.empty();
        }

        var returnToUrl = authenticationUri + "/" + _pollerPaths.getLauncherPath();

        String autostartToken = URLEncoder.encode(model.getAutoStartToken(), StandardCharsets.UTF_8);
        int launchCount = model.getLaunchCount();

        _logger.trace("Auto-start token = {}", autostartToken);
        _logger.debug("Setting launch count to {}", launchCount);

        var queryString = authenticationUri.getQuery();

        ImmutableMap.Builder<String, Object> modelBuilder = ImmutableMap.<String, Object>builder()
                .put(AUTOSTART_TOKEN, autostartToken)
                .put(RETURN_TO_URL, URLEncoder.encode(returnToUrl, StandardCharsets.UTF_8))
                .put(RESTART_URL, authenticationUri.getPath())
                .put(CANCEL_URL, authenticationUri + "/" + _pollerPaths.getCancelPath())
                .put(FAILURE_URL, authenticationUri + "/" + _pollerPaths.getFailedPath())
                .put(ACTION, authenticationUri.getPath() + "/" + _pollerPaths.getLauncherPath() + "?" + queryString)
                .put(POLL_URL, authenticationUri + "/" + _pollerPaths.getPollerPath())
                .put(CSP_OVERRIDE_CHILD_SRC, "child-src 'self' " + _launchScheme + ":;")
                .put(FORM_LAUNCH_COUNT, launchCount);

        modelUpdate.accept(modelBuilder);

        // because we invoked the webPoller above, we need to override the
        // response model key here...
        PollingResult.removePollerTypeResponseModelKey(modelBuilder);

        response.setResponseModel(new LaunchResponseModel(modelBuilder.build()), HttpStatus.OK);

        response.setHttpStatus(HttpStatus.OK);

        _sessionManager.put(Attribute.of(
                AttributeName.of(SESSION_LAUNCH_COUNT),
                launchCount + 1
        ));

        return Optional.empty();
    }

    public Optional<AuthenticationResult> post(boolean isPollingDone, Response response)
    {
        @Nullable AuthenticationResult result = _webservicePoller.getAuthenticationResult(isPollingDone, response);
        return Optional.ofNullable(result);
    }
}
