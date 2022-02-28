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

import com.google.common.collect.ImmutableMap;
import io.curity.authenticator.netid.common.client.UnknownUserNameException;
import io.curity.authenticator.netid.common.logic.PollingAuthenticatorEnterUserNameLogic;
import io.curity.authenticator.netid.common.model.UserNamePostModel;
import io.curity.authenticator.netid.model.NonValidatingUserNameRequestModel;
import se.curity.identityserver.sdk.Nullable;
import se.curity.identityserver.sdk.authentication.AuthenticatedState;
import se.curity.identityserver.sdk.authentication.AuthenticationResult;
import se.curity.identityserver.sdk.authentication.AuthenticatorRequestHandler;
import se.curity.identityserver.sdk.errors.ErrorCode;
import se.curity.identityserver.sdk.http.HttpStatus;
import se.curity.identityserver.sdk.service.ExceptionFactory;
import se.curity.identityserver.sdk.web.Request;
import se.curity.identityserver.sdk.web.Response;
import se.curity.identityserver.sdk.web.alerts.ErrorMessage;

import java.util.Optional;
import java.util.Set;

import static se.curity.identityserver.sdk.web.ResponseModel.mapResponseModel;
import static se.curity.identityserver.sdk.web.ResponseModel.templateResponseModel;

public final class EnterUserNameRequestHandler
        implements AuthenticatorRequestHandler<NonValidatingUserNameRequestModel>
{
    private final PollingAuthenticatorEnterUserNameLogic _logic;
    private final AuthenticatedState _authenticatedState;
    private final ExceptionFactory _exceptionFactory;

    public EnterUserNameRequestHandler(AuthenticatedState authenticatedState,
                                       ExceptionFactory exceptionFactory,
                                       PollingAuthenticatorEnterUserNameLogic enterUserNameLogic)
    {
        _authenticatedState = authenticatedState;
        _exceptionFactory = exceptionFactory;
        _logic = enterUserNameLogic;
    }

    @Override
    public NonValidatingUserNameRequestModel preProcess(Request request, Response response)
    {
        setResponseModel(request, response);
        return createRequestModel(request, _logic, _authenticatedState);
    }

    @Override
    public Optional<AuthenticationResult> get(NonValidatingUserNameRequestModel requestModel, Response response)
    {
        return _logic.get(requestModel, response);
    }

    @Override
    public Optional<AuthenticationResult> post(NonValidatingUserNameRequestModel requestModel, Response response)
    {
        try
        {
            return _logic.post(requestModel, response);
        }
        catch (UnknownUserNameException e)
        {
            // Stay on the same page to let the user give a new ID
            @Nullable UserNamePostModel postRequestModel = requestModel.getPostRequestModel();
            if (postRequestModel != null)
            {
                response.setResponseModel(mapResponseModel(postRequestModel.dataOnError()),
                        Response.ResponseModelScope.FAILURE);
            }
            throw _exceptionFactory.badRequestException(ErrorCode.INVALID_INPUT, "username.invalid");
        }
    }

    @Override
    public void onRequestModelValidationFailure(Request request, Response response, Set<ErrorMessage> errorMessages)
    {
        NonValidatingUserNameRequestModel model = createRequestModel(request, _logic, _authenticatedState);
        if (model.getPostRequestModel() != null)
        {
            _logic.onRequestModelValidationFailure(request, response, model.getPostRequestModel());
        }
    }

    private static NonValidatingUserNameRequestModel createRequestModel(Request request,
                                                                        PollingAuthenticatorEnterUserNameLogic logic,
                                                                        AuthenticatedState authenticatedState)
    {
        return new NonValidatingUserNameRequestModel(request, authenticatedState, logic.getUserPreferenceManager());
    }

    private void setResponseModel(Request request, Response response)
    {
        if (request.isGetRequest())
        {
            response.setResponseModel(templateResponseModel(ImmutableMap.of(),
                            "enter-username/index"),
                    Response.ResponseModelScope.NOT_FAILURE);
        }
        else if (request.isPostRequest())
        {
            response.setResponseModel(templateResponseModel(ImmutableMap.of(),
                            "enter-username/index"),
                    Response.ResponseModelScope.NOT_FAILURE);

            response.setResponseModel(templateResponseModel(ImmutableMap.of(),
                            "enter-username/index"),
                    HttpStatus.BAD_REQUEST);
        }
    }
}
