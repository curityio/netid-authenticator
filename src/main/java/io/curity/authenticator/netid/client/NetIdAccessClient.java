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

import com.google.common.base.Strings;
import com.secmaker.netid.nias.NetiDAccessServerSoap;
import com.secmaker.netid.nias.ResultCollect;
import io.curity.authenticator.netid.NetIdAccessServerSoapClient;
import io.curity.authenticator.netid.config.NetIdAccessConfig;
import jakarta.xml.ws.soap.SOAPFaultException;
import se.curity.identityserver.sdk.Nullable;
import se.curity.identityserver.sdk.plugin.ManagedObject;
import se.curity.identityserver.sdk.service.ExceptionFactory;

import static com.google.common.base.Enums.getIfPresent;
import static io.curity.authenticator.netid.client.CollectFaultStatus.INTERNAL_ERROR;
import static io.curity.authenticator.netid.utils.ClassLoaderContextUtils.withPluginClassLoader;
import static io.curity.authenticator.netid.utils.WebServiceUtils.callWebServiceWithRetry;
import static se.curity.identityserver.sdk.errors.ErrorCode.EXTERNAL_SERVICE_ERROR;

public class NetIdAccessClient extends ManagedObject<NetIdAccessConfig> implements PollingClient
{
    private static final String SERVICE_NAME = "Net iD Access";
    private final NetiDAccessServerSoap _proxy;
    private final ExceptionFactory _exceptionFactory;

    public NetIdAccessClient(NetIdAccessConfig configuration,
                             NetIdAccessServerSoapClient proxyClient)
    {
        super(configuration);
        _exceptionFactory = configuration.getExceptionFactory();
        _proxy = proxyClient.getNetIDAccessServerSoap(configuration.getTrustStore(), configuration.getClientKeyStore());
    }

    @Override
    public CollectResponse poll(String transactionId) throws PollingClientCollectException
    {
        ResultCollect response;
        try
        {
            response = callWebServiceWithRetry(
                    () -> withPluginClassLoader(() -> _proxy.collect(transactionId)),
                    () -> _exceptionFactory.
                            internalServerException(EXTERNAL_SERVICE_ERROR, "Failed to poll for status")).join();
        }
        catch (SOAPFaultException e)
        {
            String faultString = e.getFault().getFaultString();
            throw new PollingClientCollectException(e.getMessage(), e, faultString);
        }

        @Nullable CollectStatus status = getIfPresent(CollectStatus.class, response.getProgressStatus()).orNull();
        if (status == null)
        {
            throw new PollingClientCollectException("Unsuccessful poll",
                    getIfPresent(CollectFaultStatus.class, response.getProgressStatus())
                            .or(INTERNAL_ERROR));
        }

        NetIdAccessAuthenticationAttributes attributes = null;
        if (status == CollectStatus.COMPLETE)
        {
            attributes = NetIdAccessAuthenticationAttributes.of(response);
        }

        return new CollectResponse(status, attributes);
    }

    @Override
    public String getServiceName()
    {
        return SERVICE_NAME;
    }

    @Override
    public AuthenticateResponse authenticate(@Nullable String userName, boolean useSameDevice)
            throws PollingClientAuthenticateException
    {
        try
        {
            String finalUserName = Strings.nullToEmpty(userName);
            String transactionId = callWebServiceWithRetry(
                    () -> withPluginClassLoader(() -> _proxy.authenticate(finalUserName, null, null)),
                    () -> _exceptionFactory.
                            internalServerException(EXTERNAL_SERVICE_ERROR, "Failed to start authentication")).join();
            return new AuthenticateResponse.Builder(transactionId, useSameDevice ? transactionId : "").build();
        }
        catch (RuntimeException e)
        {
            Throwable cause = e.getCause();
            if (cause instanceof SOAPFaultException)
            {
                SOAPFaultException fault = (SOAPFaultException) e.getCause();
                AuthenticationFaultStatus status = getIfPresent(AuthenticationFaultStatus.class,
                        fault.getFault().getFaultString()).or(AuthenticationFaultStatus.UNKNOWN);
                throw new PollingClientAuthenticateException(fault.getMessage(), status, fault);
            }

            throw e;
        }
    }
}
