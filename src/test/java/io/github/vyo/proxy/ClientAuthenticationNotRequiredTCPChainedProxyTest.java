package io.github.vyo.proxy;

import static io.github.vyo.proxy.TransportProtocol.*;
import io.github.vyo.proxy.ChainedProxy;
import io.github.vyo.proxy.HttpProxyServerBootstrap;
import io.github.vyo.proxy.SslEngineSource;
import io.github.vyo.proxy.TransportProtocol;
import io.github.vyo.proxy.extras.SelfSignedSslEngineSource;

import javax.net.ssl.SSLEngine;

/**
 * Tests that when client authentication is not required, it doesn't matter what
 * certs the client sends.
 */
public class ClientAuthenticationNotRequiredTCPChainedProxyTest extends
        BaseChainedProxyTest {
    private final SslEngineSource serverSslEngineSource = new SelfSignedSslEngineSource(
            "chain_proxy_keystore_1.jks");

    private final SslEngineSource clientSslEngineSource = new SelfSignedSslEngineSource(
            "chain_proxy_keystore_1.jks", false, false);

    @Override
    protected HttpProxyServerBootstrap upstreamProxy() {
        return super.upstreamProxy()
                .withTransportProtocol(TCP)
                .withSslEngineSource(serverSslEngineSource)
                .withAuthenticateSslClients(false);
    }

    @Override
    protected ChainedProxy newChainedProxy() {
        return new BaseChainedProxy() {
            @Override
            public TransportProtocol getTransportProtocol() {
                return TransportProtocol.TCP;
            }

            @Override
            public boolean requiresEncryption() {
                return true;
            }

            @Override
            public SSLEngine newSslEngine() {
                return clientSslEngineSource.newSslEngine();
            }
        };
    }
}
