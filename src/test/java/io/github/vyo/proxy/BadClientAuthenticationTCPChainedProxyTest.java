package io.github.vyo.proxy;

import static io.github.vyo.proxy.TransportProtocol.*;
import io.github.vyo.proxy.ChainedProxy;
import io.github.vyo.proxy.HttpProxyServerBootstrap;
import io.github.vyo.proxy.SslEngineSource;
import io.github.vyo.proxy.TransportProtocol;
import io.github.vyo.proxy.extras.SelfSignedSslEngineSource;

import javax.net.ssl.SSLEngine;

/**
 * Tests that clients are authenticated and that if they're missing certs, we
 * get an error.
 */
public class BadClientAuthenticationTCPChainedProxyTest extends
        BaseChainedProxyTest {
    private final SslEngineSource serverSslEngineSource = new SelfSignedSslEngineSource(
            "chain_proxy_keystore_1.jks");
    
    private final SslEngineSource clientSslEngineSource = new SelfSignedSslEngineSource(
            "chain_proxy_keystore_1.jks", false, false);

    @Override
    protected boolean expectBadGatewayForEverything() {
        return true;
    }
    
    @Override
    protected HttpProxyServerBootstrap upstreamProxy() {
        return super.upstreamProxy()
                .withTransportProtocol(TCP)
                .withSslEngineSource(serverSslEngineSource);
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
