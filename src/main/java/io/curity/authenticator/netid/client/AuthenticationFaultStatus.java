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


package io.curity.authenticator.netid.client;

import io.curity.authenticator.netid.PollingAuthenticatorConstants;

import static io.curity.authenticator.netid.PollingAuthenticatorConstants.EndUserMessageKeys.unknown_error;
import static io.curity.authenticator.netid.PollingAuthenticatorConstants.EndUserMessageKeys.unknown_personal_number;

/**
 * Represents an error received from the server during the authentication call
 */
@SuppressWarnings("unused")
public enum AuthenticationFaultStatus
{
    UNKNOWN(unknown_error),
    UNKNOWN_USER(unknown_personal_number),
    INVALID_PARAMETERS(PollingAuthenticatorConstants.EndUserMessageKeys.invalid_parameters),
    ACCESS_DENIED_RP(PollingAuthenticatorConstants.EndUserMessageKeys.access_denied_rp),
    SIGN_VALIDATION_FAILED(PollingAuthenticatorConstants.EndUserMessageKeys.internal_error),
    RETRY(PollingAuthenticatorConstants.EndUserMessageKeys.internal_error),
    INTERNAL_ERROR(PollingAuthenticatorConstants.EndUserMessageKeys.internal_error),
    ALREADY_IN_PROGRESS(PollingAuthenticatorConstants.EndUserMessageKeys.in_progress),
    USER_BLOCKED(PollingAuthenticatorConstants.EndUserMessageKeys.user_blocked);
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
