package io.github.vyo.proxy;

import io.github.vyo.proxy.ProxyAuthenticator;
import io.github.vyo.proxy.extras.SelfSignedMitmManager;

/**
 * Tests a single proxy that requires username/password authentication and that
 * uses MITM.
 */
public class MITMUsernamePasswordAuthenticatingProxyTest extends
        UsernamePasswordAuthenticatingProxyTest
        implements ProxyAuthenticator {
    @Override
    protected void setUp() {
        this.proxyServer = bootstrapProxy()
                .withPort(proxyServerPort)
                .withProxyAuthenticator(this)
                .withManInTheMiddle(new SelfSignedMitmManager())
                .start();
    }

    @Override
    protected boolean isMITM() {
        return true;
    }
}