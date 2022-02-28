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

package io.curity.authenticator.netid.common.model;

import io.curity.authenticator.netid.common.utils.NullUtils;
import jakarta.validation.Valid;
import org.hibernate.validator.constraints.NotEmpty;
import org.hibernate.validator.constraints.Range;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.curity.identityserver.sdk.service.SessionManager;
import se.curity.identityserver.sdk.web.Request;

import javax.annotation.Nullable;
import java.util.Optional;

import static io.curity.authenticator.netid.common.PollingAuthenticatorConstants.FormValueNames.POLLING_DONE;
import static io.curity.authenticator.netid.common.PollingAuthenticatorConstants.SessionKeys.AUTOSTART_TOKEN;
import static io.curity.authenticator.netid.common.PollingAuthenticatorConstants.SessionKeys.INIT_TIME;
import static io.curity.authenticator.netid.common.PollingAuthenticatorConstants.SessionKeys.QR_START_SECRET;
import static io.curity.authenticator.netid.common.PollingAuthenticatorConstants.SessionKeys.QR_START_TOKEN;
import static io.curity.authenticator.netid.common.PollingAuthenticatorConstants.SessionKeys.SESSION_LAUNCH_COUNT;

public final class LaunchRequestModel
{
    private static final Logger _logger = LoggerFactory.getLogger(LaunchRequestModel.class);

    @Nullable
    @Valid
    private final Get _getRequestModel;

    @Nullable
    @Valid
    private final Post _postRequestModel;

    public LaunchRequestModel(Request request, SessionManager sessionManager)
    {
        _getRequestModel = request.isGetRequest() ? new Get(sessionManager) : null;
        _postRequestModel = request.isPostRequest() ? new Post(request) : null;
    }

    public Get getGetRequestModel()
    {
        return NullUtils.valueOrError(_getRequestModel,
                "GET RequestModel does not exist");
    }

    public Post getPostRequestModel()
    {
        return NullUtils.valueOrError(_postRequestModel,
                "POST RequestModel does not exist");
    }

    public static final class Get
    {
        @NotEmpty(message = "validation.error.autostarttoken.required")
        private final String _autoStartToken;
        @Nullable
        private final String _qrStartToken;
        @Nullable
        private final String _qrStartSecret;
        @Nullable
        private final Long _initTime;

        @Range(min = 0, max = 20)
        private final int _launchCount;

        public Get(SessionManager sessionManager)
        {
            @Nullable var autoStartTokenSession = sessionManager.get(AUTOSTART_TOKEN);
            @Nullable var qrStartTokenSession = sessionManager.get(QR_START_TOKEN);
            @Nullable var qrStartSecretSession = sessionManager.get(QR_START_SECRET);
            @Nullable var initTimeSession = sessionManager.get(INIT_TIME);

            _autoStartToken = NullUtils.mapOptionalAttribute(autoStartTokenSession, it -> it.getOptionalValueOfType(String.class), () -> "");
            _qrStartToken = NullUtils.map(qrStartTokenSession, it -> it.getValueOfType(String.class));
            _qrStartSecret = NullUtils.map(qrStartSecretSession, it -> it.getValueOfType(String.class));
            _initTime = NullUtils.map(initTimeSession, it -> it.getValueOfType(Long.class));

            _launchCount = NullUtils.mapOptionalAttribute(sessionManager.get(SESSION_LAUNCH_COUNT),
                    it -> it.getOptionalValueOfType(Integer.class), () -> 0);
        }

        public String getAutoStartToken()
        {
            return _autoStartToken;
        }

        public int getLaunchCount()
        {
            return _launchCount;
        }

        @Nullable
        public String getQrStartToken()
        {
            return _qrStartToken;
        }

        @Nullable
        public String getQrStartSecret()
        {
            return _qrStartSecret;
        }

        @Nullable
        public Long getInitTime()
        {
            return _initTime;
        }
    }

    public static final class Post
    {
        private final boolean _isPollingDone;

        public Post(Request request)
        {
            _logger.trace("transforming launch request model");

            _isPollingDone = Optional.ofNullable(request.getFormParameterValueOrError(POLLING_DONE))
                    .orElse("").equals("true");
        }

        public Post()
        {
            _isPollingDone = false;
        }

        public boolean isPollingDone()
        {
            return _isPollingDone;
        }
    }
}
