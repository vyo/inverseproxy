package io.github.vyo.proxy;

/**
 * Tests just a single basic proxy.
 */
public class SimpleProxyTest extends BaseProxyTest {
    @Override
    protected void setUp() {
        this.proxyServer = bootstrapProxy()
                .withPort(proxyServerPort)
                .start();
    }
}
