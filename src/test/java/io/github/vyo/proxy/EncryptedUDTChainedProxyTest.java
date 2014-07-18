package io.github.vyo.proxy;

import static io.github.vyo.proxy.TransportProtocol.*;
import io.github.vyo.proxy.ChainedProxy;
import io.github.vyo.proxy.HttpProxyServerBootstrap;
import io.github.vyo.proxy.SslEngineSource;
import io.github.vyo.proxy.TransportProtocol;
import io.github.vyo.proxy.extras.SelfSignedSslEngineSource;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.SSLEngine;

public class EncryptedUDTChainedProxyTest extends BaseChainedProxyTest {
    private static final AtomicInteger localPort = new AtomicInteger(61000);

    private final SslEngineSource sslEngineSource = new SelfSignedSslEngineSource(
            "chain_proxy_keystore_1.jks");

    @Override
    protected HttpProxyServerBootstrap upstreamProxy() {
        return super.upstreamProxy()
                .withTransportProtocol(UDT)
                .withSslEngineSource(sslEngineSource);
    }

    @Override
    protected ChainedProxy newChainedProxy() {
        return new BaseChainedProxy() {
            @Override
            public TransportProtocol getTransportProtocol() {
                return TransportProtocol.UDT;
            }

            @Override
            public boolean requiresEncryption() {
                return true;
            }

            @Override
            public SSLEngine newSslEngine() {
                return sslEngineSource.newSslEngine();
            }

            @Override
            public InetSocketAddress getLocalAddress() {
                return new InetSocketAddress("127.0.0.1",
                        localPort.getAndIncrement());
            }
        };
    }
}
