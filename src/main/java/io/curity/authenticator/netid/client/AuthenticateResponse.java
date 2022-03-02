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

public class AuthenticateResponse
{
    private final String _transactionId;
    private final String _autoStartToken;

    private AuthenticateResponse(Builder builder)
    {
        _transactionId = builder._transactionId;
        _autoStartToken = builder._autoStartToken;
    }

    public String getTransactionId()
    {
        return _transactionId;
    }

    public String getAutoStartToken()
    {
        return _autoStartToken;
    }

    public static final class Builder
    {
        private final String _transactionId;
        private final String _autoStartToken;

        public Builder(String transactionId, String autoStartToken)
        {
            _transactionId = transactionId;
            _autoStartToken = autoStartToken;
        }

        public AuthenticateResponse build()
        {
            return new AuthenticateResponse(this);
        }
    }
}
