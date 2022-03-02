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

import io.curity.authenticator.netid.common.PollingAuthenticatorConstants;

import static io.curity.authenticator.netid.common.PollingAuthenticatorConstants.EndUserMessageKeys.UNKNOWN_ERROR;
import static io.curity.authenticator.netid.common.PollingAuthenticatorConstants.EndUserMessageKeys.UNKNOWN_PERSONAL_NUMBER;

/**
 * Represents an error received from the server during the authentication call
 */
@SuppressWarnings("unused")
public enum AuthenticationFaultStatus
{
    UNKNOWN(UNKNOWN_ERROR),
    UNKNOWN_USER(UNKNOWN_PERSONAL_NUMBER),
    INVALID_PARAMETERS(PollingAuthenticatorConstants.EndUserMessageKeys.INVALID_PARAMETERS),
    ACCESS_DENIED_RP(PollingAuthenticatorConstants.EndUserMessageKeys.ACCESS_DENIED_RP),
    SIGN_VALIDATION_FAILED(PollingAuthenticatorConstants.EndUserMessageKeys.INTERNAL_ERROR),
    RETRY(PollingAuthenticatorConstants.EndUserMessageKeys.INTERNAL_ERROR),
    INTERNAL_ERROR(PollingAuthenticatorConstants.EndUserMessageKeys.INTERNAL_ERROR),
    ALREADY_IN_PROGRESS(PollingAuthenticatorConstants.EndUserMessageKeys.IN_PROGRESS);
    /**
     * Message key to show user
     */
    private final String _messageId;

    AuthenticationFaultStatus(String messageId)
    {
        _messageId = messageId;
    }

    public String getMessageId()
    {
        return _messageId;
    }
}
