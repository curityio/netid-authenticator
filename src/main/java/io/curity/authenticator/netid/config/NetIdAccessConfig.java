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

package io.curity.authenticator.netid.config;

import se.curity.identityserver.sdk.config.Configuration;
import se.curity.identityserver.sdk.config.annotation.DefaultBoolean;
import se.curity.identityserver.sdk.config.annotation.DefaultInteger;
import se.curity.identityserver.sdk.config.annotation.DefaultString;
import se.curity.identityserver.sdk.config.annotation.Description;
import se.curity.identityserver.sdk.config.annotation.Name;
import se.curity.identityserver.sdk.service.ExceptionFactory;
import se.curity.identityserver.sdk.service.SessionManager;
import se.curity.identityserver.sdk.service.UserPreferenceManager;
import se.curity.identityserver.sdk.service.authentication.AuthenticatorInformationProvider;
import se.curity.identityserver.sdk.service.crypto.ClientKeyCryptoStore;
import se.curity.identityserver.sdk.service.crypto.ServerTrustCryptoStore;

import java.util.Optional;

public interface NetIdAccessConfig extends Configuration
{

    @Name("hostname")
    @DefaultString("showroom.lab.secmaker.com")
    @Description("Sets the hostname or ip-address of the Net iD Access service.")
    String getHostName();

    @Name("port")
    @DefaultInteger(443)
    @Description("Sets the port of the Net iD Access service.")
    int getPort();

    @Name("path")
    @DefaultString("/nias/ServiceServer.asmx")
    @Description("Sets the full path to the Net iD Access service.")
    String getPath();

    @Description("The Net iD Access service trust store.")
    Optional<ServerTrustCryptoStore> getTrustStore();

    @Description("The keystore that will be used in connections to Net iD Access service.")
    Optional<ClientKeyCryptoStore> getClientKeyStore();

    @DefaultBoolean(false)
    @Description("Whether to use an HTTP connection to the Net iD Access service. Defaults to an HTTPS connection.")
    Boolean isDisableHttps();

    SessionManager getSessionManager();

    ExceptionFactory getExceptionFactory();

    AuthenticatorInformationProvider getAuthenticatorInformationProvider();

    UserPreferenceManager getUserPreferenceManager();
}
