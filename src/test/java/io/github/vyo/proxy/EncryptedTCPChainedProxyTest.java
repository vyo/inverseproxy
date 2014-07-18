package io.github.vyo.proxy;

import static io.github.vyo.proxy.TransportProtocol.*;
import io.github.vyo.proxy.ChainedProxy;
import io.github.vyo.proxy.HttpProxyServerBootstrap;
import io.github.vyo.proxy.SslEngineSource;
import io.github.vyo.proxy.TransportProtocol;
import io.github.vyo.proxy.extras.SelfSignedSslEngineSource;

import javax.net.ssl.SSLEngine;

public class EncryptedTCPChainedProxyTest extends BaseChainedProxyTest {
    private final SslEngineSource sslEngineSource = new SelfSignedSslEngineSource(
            "chain_proxy_keystore_1.jks");

    @Override
    protected HttpProxyServerBootstrap upstreamProxy() {
        return super.upstreamProxy()
                .withTransportProtocol(TCP)
                .withSslEngineSource(sslEngineSource);
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
                return sslEngineSource.newSslEngine();
            }
        };
    }
}
