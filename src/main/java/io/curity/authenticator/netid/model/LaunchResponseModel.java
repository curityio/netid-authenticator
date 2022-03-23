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

import io.curity.authenticator.netid.utils.NullUtils;
import se.curity.identityserver.sdk.web.ResponseModel;

import java.util.Map;

import static io.curity.authenticator.netid.PollingAuthenticatorConstants.FormValueNames.AUTOSTART_TOKEN;
import static io.curity.authenticator.netid.PollingAuthenticatorConstants.FormValueNames.CANCEL_URL;
import static io.curity.authenticator.netid.PollingAuthenticatorConstants.FormValueNames.FAILURE_URL;
import static io.curity.authenticator.netid.PollingAuthenticatorConstants.FormValueNames.FORM_LAUNCH_COUNT;
import static io.curity.authenticator.netid.PollingAuthenticatorConstants.FormValueNames.POLL_URL;
import static io.curity.authenticator.netid.PollingAuthenticatorConstants.FormValueNames.RESTART_URL;
import static io.curity.authenticator.netid.PollingAuthenticatorConstants.FormValueNames.RETURN_TO_URL;
import static io.curity.authenticator.netid.utils.SdkConstants.ACTION;
import static io.curity.authenticator.netid.utils.SdkConstants.CSP_OVERRIDE_CHILD_SRC;


public final class LaunchResponseModel implements ResponseModel
{

    private final Map<String, Object> _map;

    public LaunchResponseModel(Map<String, Object> map)
    {
        _map = map;
    }

    public String getAutoStartToken()
    {
        return NullUtils.valueOfTypeOrError(String.class, _map.get(AUTOSTART_TOKEN),
                "autoStartToken is null");
    }

    public String getReturnToUrl()
    {
        return NullUtils.valueOfTypeOrError(String.class, _map.get(RETURN_TO_URL),
                "returnToUrl is null");
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

    public String getCspOverrideChildSrc()
    {
        return NullUtils.valueOfTypeOrError(String.class, _map.get(CSP_OVERRIDE_CHILD_SRC),
                "cspOverrideChildSrc is null");
    }

    public int getFormLaunchCount()
    {
        return NullUtils.valueOfTypeOrError(Integer.class, _map.get(FORM_LAUNCH_COUNT),
                "formLaunchCount is null");
    }

    @Override
    public Map<String, Object> getViewData()
    {
        return _map;
    }
}
