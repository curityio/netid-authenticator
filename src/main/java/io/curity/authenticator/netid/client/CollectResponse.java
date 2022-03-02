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

import io.curity.authenticator.netid.utils.NullUtils;
import se.curity.identityserver.sdk.attribute.AuthenticationAttributes;
import se.curity.identityserver.sdk.attribute.SubjectAttributes;

import javax.annotation.Nullable;

public class CollectResponse
{
    private final CollectStatus _status;

    @Nullable
    private final transient AuthenticationAttributes _authenticationAttributes;

    @Nullable
    private final String _subject;

    public CollectResponse(CollectStatus collectStatus)
    {
        this(collectStatus, null);
    }

    public CollectResponse(CollectStatus collectStatus, @Nullable AuthenticationAttributes authenticationAttributes)
    {
        _status = collectStatus;
        if (authenticationAttributes != null)
        {
            _subject = authenticationAttributes.getSubject();
            _authenticationAttributes = authenticationAttributes;
        }
        else
        {
            _subject = null;
            _authenticationAttributes = null;
        }
    }

    public CollectStatus getStatus()
    {
        return _status;
    }

    /**
     * Get the authentication attributes
     *
     * @param subject Subject to use
     * @return AuthenticationAttributes
     */
    public AuthenticationAttributes getAuthenticationAttributes(String subject)
    {
        AuthenticationAttributes authenticationAttributes = NullUtils.valueOrError(_authenticationAttributes,
                "Authentication Attributes was not collected");

        if (!subject.equals(_subject))
        {
            return AuthenticationAttributes.of(
                    SubjectAttributes.of(subject, authenticationAttributes.getSubjectAttributes()),
                    authenticationAttributes.getContextAttributes());
        }
        return authenticationAttributes;
    }

    @Nullable
    public String getSubject()
    {
        return _subject;
    }
}
