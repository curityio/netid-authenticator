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
import se.curity.identityserver.sdk.web.ResponseModel;

import java.util.Map;

import static io.curity.authenticator.netid.common.PollingAuthenticatorConstants.FormValueNames.CANCEL_URL;
import static io.curity.authenticator.netid.common.PollingAuthenticatorConstants.FormValueNames.FAILURE_URL;
import static io.curity.authenticator.netid.common.PollingAuthenticatorConstants.FormValueNames.POLL_URL;
import static io.curity.authenticator.netid.common.PollingAuthenticatorConstants.FormValueNames.RESTART_URL;
import static io.curity.authenticator.netid.common.PollingAuthenticatorConstants.FormValueNames.SERVICE_MESSAGE;
import static io.curity.authenticator.netid.common.utils.SdkConstants.ACTION;

public final class WaitResponseModel implements ResponseModel
{
    private final Map<String, Object> _map;

    public WaitResponseModel(Map<String, Object> map)
    {
        _map = map;
    }

    public String getRestartUrl()
    {
        return NullUtils.valueOfTypeOrError(String.class, _map.get(RESTART_URL),
                "restartUrl is null");
    }

    public String getFailureUrl()
    {
        return NullUtils.valueOfTypeOrError(String.class, _map.get(FAILURE_URL),
                "failureUrl is null");
    }

    public String getCancelUrl()
    {
        return NullUtils.valueOfTypeOrError(String.class, _map.get(CANCEL_URL),
                "cancelUrl is null");
    }

    public String getAction()
    {
        return NullUtils.valueOfTypeOrError(String.class, _map.get(ACTION),
                "action is null");
    }

    public String getPollUrl()
    {
        return NullUtils.valueOfTypeOrError(String.class, _map.get(POLL_URL),
                "pollUrl is null");
    }

    public String getServiceMessage()
    {
        return NullUtils.valueOfTypeOrError(String.class, _map.get(SERVICE_MESSAGE),
                "serviceMessage is null");
    }

    @Override
    public Map<String, Object> getViewData()
    {
        return _map;
    }
}
