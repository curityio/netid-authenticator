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

package io.curity.authenticator.netid.model;

import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import se.curity.identityserver.sdk.Nullable;
import se.curity.identityserver.sdk.authentication.AuthenticatedState;
import se.curity.identityserver.sdk.service.UserPreferenceManager;
import se.curity.identityserver.sdk.web.Request;

import java.util.Map;

import static org.apache.commons.lang3.StringEscapeUtils.unescapeHtml4;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

public final class NonValidatingUserNameRequestModel extends UserNameRequestModel
{
    public NonValidatingUserNameRequestModel(Request request,
                                             AuthenticatedState authenticatedState,
                                             UserPreferenceManager userPreferenceManager)
    {
        _postRequestModel = request.isPostRequest() ?
                new NonValidatingUserNamePostModel(request, userPreferenceManager) : null;
        _getRequestModel = request.isGetRequest() ? new NonValidatingUserNameGetModel(authenticatedState) : null;
    }

    private static final class NonValidatingUserNameGetModel implements UserNameGetModel
    {

        private final AuthenticatedState _authenticatedState;

        public NonValidatingUserNameGetModel(AuthenticatedState authenticatedState)
        {

            _authenticatedState = authenticatedState;
        }

        @Override
        @Nullable
        public String getAuthenticatedUsername()
        {
            return _authenticatedState.isAuthenticated() ? _authenticatedState.getUsername() : null;
        }
    }

    private static final class NonValidatingUserNamePostModel implements UserNamePostModel
    {
        private static final String IDENTIFIER_PARAM = "userName";
        private static final String USE_SAME_DEVICE_PARAM = "usesamedevice";

        @Nullable
        private final String _userName;

        private final boolean _useSameDevice;

        private static final Logger _logger = LoggerFactory.getLogger(NonValidatingUserNamePostModel.class);
        private static final Marker MASK_MARKER = MarkerFactory.getMarker("MASK");

        NonValidatingUserNamePostModel(Request request,
                                       UserPreferenceManager userPreferenceManager)
        {

            @Nullable String userName = request.getFormParameterValueOrError(IDENTIFIER_PARAM);

            if (isEmpty(userName) && useOtherDevice() && isNotEmpty(userPreferenceManager.getUsername()))
            {
                _userName = userPreferenceManager.getUsername();
            }
            else
            {
                _userName = userName;
            }

            if (isNotEmpty(_userName))
            {
                _logger.debug(MASK_MARKER, "Found username {}", _userName);
            }

            @Nullable String useSameDevice = request.getFormParameterValueOrError(USE_SAME_DEVICE_PARAM);
            _useSameDevice = Boolean.parseBoolean(useSameDevice);
        }

        @Override
        public boolean isValid()
        {
            return useSameDevice() || isNotEmpty(_userName);
        }

        public Map<String, Object> dataOnError()
        {
            return ImmutableMap.of(IDENTIFIER_PARAM, unescapeHtml4(_userName));
        }

        @Nullable
        public String getUserName()
        {
            return useSameDevice() ? null : _userName;
        }

        public boolean useSameDevice()
        {
            return _useSameDevice;
        }
    }
}
