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

package io.curity.authenticator.netid;

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
import se.curity.identityserver.sdk.plugin.ManagedObject;
import se.curity.identityserver.sdk.service.crypto.ClientKeyCryptoStore;
import se.curity.identityserver.sdk.service.crypto.ServerTrustCryptoStore;

import javax.annotation.Nullable;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.xml.namespace.QName;
import java.io.ByteArrayOutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static io.curity.authenticator.netid.utils.ClassLoaderContextUtils.withPluginClassLoader;
import static javax.net.ssl.KeyManagerFactory.getDefaultAlgorithm;

public final class NetIdAccessServerSoapClient extends ManagedObject<NetIdAccessConfig>
{
    private NetiDAccessServerSoap _netIDAccessServerSoap = null;
    private final NetIdAccessConfig _config;

    private static final Logger _logger = LoggerFactory.getLogger(NetIdAccessServerSoapClient.class);

    private static final String JAXWS_PROPERTIES_SSL_SOCKET_FACTORY = "com.sun.xml.ws.transport.https.client.SSLSocketFactory";
    private static final String JAXWS_PROPERTIES_CONNECT_TIMEOUT = "com.sun.xml.ws.connect.timeout";
    private static final String JAXWS_PROPERTIES_REQUEST_TIMEOUT = "com.sun.xml.ws.request.timeout";

    // Generous timeouts; we only want to make sure that the requesting thread isn't consumed indefinitely.
    private static final int CONNECT_TIMEOUT = 3000;
    private static final int REQUEST_TIMEOUT = 10000;

    public NetIdAccessServerSoapClient(NetIdAccessConfig configuration)
    {
        super(configuration);
        _config = configuration;
    }

    private SSLSocketFactory getSSLSocketFactory(Optional<ServerTrustCryptoStore> maybeTrustStore, Optional<ClientKeyCryptoStore> maybeClientKeyStore)
    {
        @Nullable TrustManager[] trustManagers = null;
        @Nullable KeyManager[] keyManagers = null;

        try
        {
            if (maybeTrustStore.isPresent())
            {
                _logger.debug("Applying ssl server-truststore from configuration.");

                var trustStore = maybeTrustStore.get().getAsKeyStore();

                var trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                trustManagerFactory.init(trustStore);
                trustManagers = trustManagerFactory.getTrustManagers();
            }

            if (maybeClientKeyStore.isPresent())
            {
                var keyStore = maybeClientKeyStore.get();
                _logger.debug("Applying ssl client-keystore from configuration.");

                var keyManagerFactory = KeyManagerFactory.getInstance(getDefaultAlgorithm());
                keyManagerFactory.init(keyStore.getAsKeyStore(), keyStore.getKeyStorePassword());
                keyManagers = keyManagerFactory.getKeyManagers();
            }

            var sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagers, trustManagers, null);

            return sslContext.getSocketFactory();
        }
        catch (KeyStoreException | NoSuchAlgorithmException | KeyManagementException | UnrecoverableKeyException e)
        {
            _logger.info("Could not create SSL Context: {}", e.getMessage(), e);
            throw new SSLContextException();
        }
    }

    private void configureWebserviceClient(BindingProvider bindingProvider,
                                           SSLSocketFactory socketFactory)
    {
        var bindingProviderRequestContext = bindingProvider.getRequestContext();

        //Override the endpoint in the WSDL with the configured endpoint
        bindingProviderRequestContext.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, buildEndpointAddress());

        bindingProviderRequestContext.put(JAXWS_PROPERTIES_CONNECT_TIMEOUT, CONNECT_TIMEOUT);
        bindingProviderRequestContext.put(JAXWS_PROPERTIES_REQUEST_TIMEOUT, REQUEST_TIMEOUT);

        if (!_config.isDisableHttps())
        {
            bindingProvider.getRequestContext().put(JAXWS_PROPERTIES_SSL_SOCKET_FACTORY, socketFactory);
        }

        if (_logger.isTraceEnabled())
        {
            Binding binding = bindingProvider.getBinding();
            @SuppressWarnings("rawtypes") List<Handler> handlerChain = binding.getHandlerChain();

            handlerChain.add(new SOAPHandler<>()
            {
                @Override
                public boolean handleMessage(SOAPMessageContext context)
                {
                    return withPluginClassLoader(() -> {
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
                    });
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

    private String buildEndpointAddress()
    {
        var scheme = _config.isDisableHttps() ? "http" : "https";
        try
        {
            return new URL(scheme, _config.getHostName(), _config.getPort(), _config.getPath()).toString();
        }
        catch (MalformedURLException e)
        {
            throw new IllegalArgumentException("Could not build URL to Net iD Access server: " + e.getMessage(), e);
        }
    }

    public NetiDAccessServerSoap getNetIDAccessServerSoap(Optional<ServerTrustCryptoStore> maybeTrustStore, Optional<ClientKeyCryptoStore> maybeClientKeyStore)
    {
        if (_netIDAccessServerSoap == null)
        {
            _netIDAccessServerSoap = withPluginClassLoader(() -> {
                NetiDAccessServer accessServer = new NetiDAccessServer();
                NetiDAccessServerSoap proxy = accessServer.getNetiDAccessServerSoap();
                configureWebserviceClient(
                        (BindingProvider) proxy,
                        getSSLSocketFactory(maybeTrustStore, maybeClientKeyStore)
                );
                return proxy;
            });
        }

        return _netIDAccessServerSoap;
    }
}
