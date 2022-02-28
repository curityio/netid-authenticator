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

package io.curity.authenticator.netid.injectors;

import com.secmaker.netid.nias.NetiDAccessServer;
import com.secmaker.netid.nias.NetiDAccessServerSoap;
import io.curity.authenticator.netid.config.NetIdAccessConfig;
import jakarta.xml.soap.SOAPMessage;
import jakarta.xml.ws.Binding;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.handler.Handler;
import jakarta.xml.ws.handler.MessageContext;
import jakarta.xml.ws.handler.soap.SOAPHandler;
import jakarta.xml.ws.handler.soap.SOAPMessageContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.namespace.QName;
import java.io.ByteArrayOutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class NetIdAccessServerSoapFactory
{
    private static final Logger _logger = LoggerFactory.getLogger(NetIdAccessServerSoapFactory.class);

    private static final String JAXWS_PROPERTIES_CONNECT_TIMEOUT = "com.sun.xml.ws.connect.timeout";
    private static final String JAXWS_PROPERTIES_REQUEST_TIMEOUT = "com.sun.xml.ws.request.timeout";

    // Generous timeouts; we only want to make sure that the requesting thread isn't consumed indefinitely.
    private static final int CONNECT_TIMEOUT = 3000;
    private static final int REQUEST_TIMEOUT = 10000;

    private final NetIdAccessConfig _config;

    public NetIdAccessServerSoapFactory(NetIdAccessConfig config)
    {
        _config = config;
    }

    public NetiDAccessServerSoap create()
    {
        var httpClient = _config.getHttpClient();

        NetiDAccessServer accessServer = new NetiDAccessServer();
        NetiDAccessServerSoap proxy = accessServer.getNetiDAccessServerSoap();

        configureWebserviceClient((BindingProvider) proxy, buildEndpointAddress(httpClient.getScheme()));

        return proxy;
    }

    private static void configureWebserviceClient(BindingProvider bindingProvider,
                                                 String endpoint)
    {
        Map<String, Object> bindingProviderRequestContext = bindingProvider.getRequestContext();

        //Override the endpoint in the WSDL with the configured endpoint
        bindingProviderRequestContext.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpoint);

        bindingProviderRequestContext.put(JAXWS_PROPERTIES_CONNECT_TIMEOUT, CONNECT_TIMEOUT);
        bindingProviderRequestContext.put(JAXWS_PROPERTIES_REQUEST_TIMEOUT, REQUEST_TIMEOUT);


        if (_logger.isTraceEnabled())
        {
            Binding binding = bindingProvider.getBinding();
            @SuppressWarnings("rawtypes") List<Handler> handlerChain = binding.getHandlerChain();

            handlerChain.add(new SOAPHandler<>()
            {
                @Override
                public boolean handleMessage(SOAPMessageContext context)
                {
                    SOAPMessage soapMessage = context.getMessage();

                    try
                    {
                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

                        soapMessage.writeTo(outputStream);

                        _logger.trace(outputStream.toString(StandardCharsets.UTF_8));
                    }
                    catch (Exception ex)
                    {
                        _logger.debug("Could not log SOAP message", ex);
                    }

                    return true;
                }

                @Override
                public boolean handleFault(SOAPMessageContext context)
                {
                    return true;
                }

                @Override
                public void close(MessageContext context)
                {
                }

                @Override
                public Set<QName> getHeaders()
                {
                    return Collections.emptySet();
                }
            });

            binding.setHandlerChain(handlerChain);
        }
    }

    private String buildEndpointAddress(String scheme)
    {
        try
        {
            return new URL(scheme, _config.getHostName(), _config.getPort(), _config.getPath()).toString();
        }
        catch (MalformedURLException e)
        {
            throw new IllegalArgumentException("Could not build URL to Net iD Access server: " + e.getMessage(), e);
        }
    }
}
