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

import static io.curity.authenticator.netid.PollingAuthenticatorConstants.EndUserMessageKeys.user_cancelled;

/**
 * Represents an error received from the server during the collect call
 */
@SuppressWarnings("unused")
public enum CollectFaultStatus
{
    ACCESS_DENIED_RP(PollingAuthenticatorConstants.EndUserMessageKeys.access_denied_rp, false),
    INVALID_PARAMETERS(PollingAuthenticatorConstants.EndUserMessageKeys.invalid_parameters, false),
    CLIENT_ERR(PollingAuthenticatorConstants.EndUserMessageKeys.client_error, true),
    CERTIFICATE_ERR(PollingAuthenticatorConstants.EndUserMessageKeys.certificate_error, true),
    RETRY(PollingAuthenticatorConstants.EndUserMessageKeys.internal_error, true),
    INTERNAL_ERROR(PollingAuthenticatorConstants.EndUserMessageKeys.internal_error, true),
    EXPIRED_TRANSACTION(PollingAuthenticatorConstants.EndUserMessageKeys.expired_transaction, true),
    USER_CANCEL(user_cancelled, true),
    CANCELLED(PollingAuthenticatorConstants.EndUserMessageKeys.cancelled, true),
    START_FAILED(PollingAuthenticatorConstants.EndUserMessageKeys.start_failed, true);

    /**
     * Message key to show user
     */
    private final String _messageId;

    /**
     * Whether this is a fatal error. If not, polling may continue as it may still succeed.
     */
    private boolean _isFatal;

    CollectFaultStatus(String messageId, boolean isFatal)
    {
        _messageId = messageId;
    }

    public String getMessageId()
    {
        return _messageId;
    }

    public boolean isFatal()
    {
        return _isFatal;
    }
}
