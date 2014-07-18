package io.github.vyo.proxy;

import static io.github.vyo.proxy.TransportProtocol.*;
import io.github.vyo.proxy.ChainedProxy;
import io.github.vyo.proxy.HttpProxyServerBootstrap;
import io.github.vyo.proxy.TransportProtocol;

public class UnencryptedUDTChainedProxyTest extends BaseChainedProxyTest {
    @Override
    protected HttpProxyServerBootstrap upstreamProxy() {
        return super.upstreamProxy()
                .withTransportProtocol(UDT);
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
                return false;
            }
        };
    }
}
