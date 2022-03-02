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

package io.curity.authenticator.netid.descriptor;

import com.google.common.collect.ImmutableMap;
import io.curity.authenticator.netid.client.NetIdAccessClient;
import io.curity.authenticator.netid.config.NetIdAccessConfig;
import io.curity.authenticator.netid.endpoints.authenticate.CancelRequestHandler;
import io.curity.authenticator.netid.endpoints.authenticate.EnterUserNameRequestHandler;
import io.curity.authenticator.netid.endpoints.authenticate.FailedRequestHandler;
import io.curity.authenticator.netid.endpoints.authenticate.LaunchRequestHandler;
import io.curity.authenticator.netid.endpoints.authenticate.WaitRequestHandler;
import io.curity.authenticator.netid.injectors.NetIdAccessServerSoapFactory;
import se.curity.identityserver.sdk.authentication.AuthenticatorRequestHandler;
import se.curity.identityserver.sdk.plugin.descriptor.AuthenticatorPluginDescriptor;

import java.util.Optional;

import static io.curity.authenticator.netid.common.PollingAuthenticatorConstants.Endpoints.CANCEL;
import static io.curity.authenticator.netid.common.PollingAuthenticatorConstants.Endpoints.FAILED;
import static io.curity.authenticator.netid.common.PollingAuthenticatorConstants.Endpoints.LAUNCH;
import static io.curity.authenticator.netid.common.PollingAuthenticatorConstants.Endpoints.WAIT;

public final class NetIdAccessAuthenticatorPluginDescriptor
        implements AuthenticatorPluginDescriptor<NetIdAccessConfig>
{
    @Override
    public Class<? extends NetIdAccessConfig> getConfigurationType()
    {
        return NetIdAccessConfig.class;
    }

    public ImmutableMap<String, Class<? extends AuthenticatorRequestHandler<?>>> getAuthenticationRequestHandlerTypes()
    {
        return ImmutableMap.of(
                "index", EnterUserNameRequestHandler.class,
                WAIT, WaitRequestHandler.class,
                FAILED, FailedRequestHandler.class,
                LAUNCH, LaunchRequestHandler.class,
                CANCEL, CancelRequestHandler.class
        );
    }

    @Override
    public String getPluginImplementationType()
    {
        // TODO - this should be "netidaccess" before publishing the plugin.
        // For now it's different so that both plugins can be used in CIS for testing.
        return "netidaccess-os";
    }

    @Override
    public Optional<NetIdAccessClient> createManagedObject(NetIdAccessConfig configuration)
    {
        return Optional.of(new NetIdAccessClient(configuration, new NetIdAccessServerSoapFactory(configuration)));
    }
}
