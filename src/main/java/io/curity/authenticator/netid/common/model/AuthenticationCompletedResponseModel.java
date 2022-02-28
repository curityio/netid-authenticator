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

import com.google.common.collect.ImmutableMap;
import se.curity.identityserver.sdk.web.ResponseModel;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;

import static io.curity.authenticator.netid.common.PollingAuthenticatorConstants.ResponseModelFields.AUTHN_COMPLETE;
import static io.curity.authenticator.netid.common.utils.SdkConstants.ACTION;

public final class AuthenticationCompletedResponseModel implements ResponseModel
{
    private final String _action;
    private final boolean _done;

    public AuthenticationCompletedResponseModel(String action, boolean authenticationDone)
    {
        _action = action;
        _done = authenticationDone;
    }

    public String getAction()
    {
        return _action;
    }

    public boolean isDone()
    {
        return _done;
    }

    public static Optional<AuthenticationCompletedResponseModel> tryConvert(Map<String, Object> model)
    {
        @Nullable Object action = model.get(ACTION);
        @Nullable Object isComplete = model.get(AUTHN_COMPLETE);

        if (action instanceof String && isComplete instanceof Boolean)
        {
            return Optional.of(new AuthenticationCompletedResponseModel(
                    (String) action, (Boolean) isComplete));
        }

        return Optional.empty();
    }

    @Override
    public Map<String, Object> getViewData()
    {
        return ImmutableMap.of("_action", _action, AUTHN_COMPLETE, _done);
    }
}
