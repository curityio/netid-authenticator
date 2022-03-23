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

import io.curity.authenticator.netid.InvalidParameterException;
import io.curity.authenticator.netid.PollingAuthenticatorConstants;
import io.curity.authenticator.netid.utils.NullUtils;
import jakarta.validation.Valid;
import org.hibernate.validator.constraints.NotEmpty;
import se.curity.identityserver.sdk.service.SessionManager;
import se.curity.identityserver.sdk.web.Request;

import javax.annotation.Nullable;

import static io.curity.authenticator.netid.PollingAuthenticatorConstants.EndUserMessageKeys.UNKNOWN_ERROR;

public final class FailedRequestModel
{

    @Nullable
    @Valid
    private final Get _getRequestModel;

    public FailedRequestModel(Request request, SessionManager sessionManager)
    {
        if (request.isGetRequest())
        {
            _getRequestModel = new Get(request, sessionManager);
        }
        else
        {
            _getRequestModel = null;
        }
    }

    public Get getGetRequestModel()
    {
        return NullUtils.valueOrError(_getRequestModel,
                "GET RequestModel does not exist");
    }

    public static final class Get
    {
        private final String _incomingErrorMessage;

        @Nullable
        private final String _errorMessage;

        Get(Request request, SessionManager sessionManager)
        {
            var listOfMessages = request.getQueryParameterValues(PollingAuthenticatorConstants.FormValueNames.ERROR_MESSAGE);

            if (listOfMessages != null && listOfMessages.size() > 0)
            {
                if (listOfMessages.size() > 1)
                {
                    throw new InvalidParameterException(PollingAuthenticatorConstants.FormValueNames.ERROR_MESSAGE, "Invalid parameter");
                }
                _incomingErrorMessage = listOfMessages.stream().findFirst().orElse(UNKNOWN_ERROR);
            }
            else
            {
                _incomingErrorMessage = UNKNOWN_ERROR;
            }

            if (_incomingErrorMessage.equals(UNKNOWN_ERROR))
            {
                @Nullable var messageFromSession = sessionManager.get(PollingAuthenticatorConstants.SessionKeys.ERROR_MESSAGE);

                if (messageFromSession != null)
                {
                    _errorMessage = messageFromSession.getValueOfType(String.class);
                }
                else
                {
                    _errorMessage = UNKNOWN_ERROR;
                }
            }
            else
            {
                _errorMessage = null;
            }
        }

        @NotEmpty
        public String getErrorMessage()
        {
            @Nullable String errorMessage = _incomingErrorMessage.equals(UNKNOWN_ERROR) ? _errorMessage : _incomingErrorMessage;

            return errorMessage == null || "[object Object]".equals(errorMessage) ? UNKNOWN_ERROR : errorMessage;
        }
    }
}
