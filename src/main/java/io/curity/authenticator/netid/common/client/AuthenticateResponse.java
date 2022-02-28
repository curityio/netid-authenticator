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


package io.curity.authenticator.netid.common.client;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class AuthenticateResponse
{
    private final String _transactionId;
    private final String _autoStartToken;
    @Nullable private final String _qrStartToken;
    @Nullable private final String _qrStartSecret;

    private AuthenticateResponse(Builder builder)
    {
        _transactionId = builder._transactionId;
        _autoStartToken = builder._autoStartToken;
        _qrStartToken = builder._qrStartToken;
        _qrStartSecret = builder._qrStartSecret;
    }

    public String getTransactionId()
    {
        return _transactionId;
    }

    public String getAutoStartToken()
    {
        return _autoStartToken;
    }

    @Nullable
    public String getQrStartToken()
    {
        return _qrStartToken;
    }

    @Nullable
    public String getQrStartSecret()
    {
        return _qrStartSecret;
    }

    public static final class Builder
    {
        private final String _transactionId;
        private final String _autoStartToken;
        @Nullable private String _qrStartToken = null;
        @Nullable private String _qrStartSecret = null;

        public Builder(String transactionId, String autoStartToken)
        {
            _transactionId = transactionId;
            _autoStartToken = autoStartToken;
        }

        public Builder setQrStartToken(@Nonnull String qrStartToken)
        {
            _qrStartToken = qrStartToken;
            return this;
        }

        public Builder setQrStartSecret(@Nonnull String qrStartSecret)
        {
            _qrStartSecret = qrStartSecret;
            return this;
        }

        public AuthenticateResponse build()
        {
            return new AuthenticateResponse(this);
        }
    }
}
