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

import static io.curity.authenticator.netid.PollingAuthenticatorConstants.EndUserMessageKeys.NO_APP;
import static io.curity.authenticator.netid.PollingAuthenticatorConstants.EndUserMessageKeys.NO_APP_TRY_OTHER_DEVICE;
import static io.curity.authenticator.netid.PollingAuthenticatorConstants.EndUserMessageKeys.START_APP;

public enum CollectStatus
{
    OUTSTANDING_TRANSACTION(PollingAuthenticatorConstants.EndUserMessageKeys.OUTSTANDING_TRANSACTION, START_APP),
    NO_CLIENT(START_APP),
    STARTED(NO_APP_TRY_OTHER_DEVICE, NO_APP),
    USER_SIGN(PollingAuthenticatorConstants.EndUserMessageKeys.USER_SIGN),
    COMPLETE(""),
    PENDING(""),
    FAILED("");

    private final String _sameDeviceMessageId;
    private final String _otherDeviceMessageId;

    CollectStatus(String messageId)
    {
        this(messageId, messageId);
    }

    CollectStatus(String sameDeviceMessageId, String otherDeviceMessageId)
    {
        _sameDeviceMessageId = sameDeviceMessageId;
        _otherDeviceMessageId = otherDeviceMessageId;
    }

    public String getSameDeviceMessageId()
    {
        return _sameDeviceMessageId;
    }

    public String getOtherDeviceMessageId()
    {
        return _otherDeviceMessageId;
    }
}
