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

package io.curity.authenticator.netid.endpoints.authenticate;

import io.curity.authenticator.netid.common.logic.LaunchRequestLogic;
import io.curity.authenticator.netid.common.model.LaunchRequestModel;
import se.curity.identityserver.sdk.authentication.AuthenticationResult;
import se.curity.identityserver.sdk.authentication.AuthenticatorRequestHandler;
import se.curity.identityserver.sdk.web.Request;
import se.curity.identityserver.sdk.web.Response;

import java.util.Optional;

public final class LaunchRequestHandler implements AuthenticatorRequestHandler<LaunchRequestModel>
{
    public static final String SCHEME = "netid";
    private final LaunchRequestLogic _logic;

    public LaunchRequestHandler(LaunchRequestLogic logic)
    {
        _logic = logic;
    }

    @Override
    public LaunchRequestModel preProcess(Request request, Response response)
    {
        return _logic.preProcess(request, response);
    }

    @Override
    public Optional<AuthenticationResult> get(LaunchRequestModel requestModel, Response response)
    {
        return _logic.get(requestModel, response);
    }

    @Override
    public Optional<AuthenticationResult> post(LaunchRequestModel requestModel, Response response)
    {
        return _logic.post(requestModel.getPostRequestModel().isPollingDone(), response);
    }
}
