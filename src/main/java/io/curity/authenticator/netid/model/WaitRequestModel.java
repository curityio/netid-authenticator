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
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.curity.identityserver.sdk.web.Request;

import javax.annotation.Nullable;
import java.util.Optional;

import static io.curity.authenticator.netid.PollingAuthenticatorConstants.FormValueNames.POLLING_DONE;


public final class WaitRequestModel
{
    private static final Logger _logger = LoggerFactory.getLogger(WaitRequestModel.class);

    @Valid
    @Nullable
    private final Post _postRequestModel;

    public WaitRequestModel(Request request)
    {
        _postRequestModel = request.isPostRequest() ? new Post(request) : null;
    }

    public Post getPostRequestModel()
    {
        return NullUtils.valueOrError(_postRequestModel,
                "POST RequestModel does not exist");
    }

    public static final class Post
    {
        private final boolean _isPollingDone;

        public Post(Request request)
        {
            _logger.trace("transforming wait request model");

            _isPollingDone = Optional.ofNullable(request.getFormParameterValueOrError(POLLING_DONE))
                    .orElse("").equals("true");
        }

        public boolean isPollingDone()
        {
            return _isPollingDone;
        }
    }
}
