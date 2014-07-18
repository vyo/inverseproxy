package io.github.vyo.proxy.impl;

import org.junit.Test;

import static io.github.vyo.proxy.impl.ProxyUtils.*;
import static org.junit.Assert.assertEquals;

/**
 * Test for proxy utilities.
 */
public class ProxyUtilsTest {

    @Test
    public void testParseHostAndPort() throws Exception {
        assertEquals("www.test.com:80", parseHostAndPort("http://www.test.com:80/test"));
        assertEquals("www.test.com:80", parseHostAndPort("https://www.test.com:80/test"));
        assertEquals("www.test.com:443", parseHostAndPort("https://www.test.com:443/test"));
        assertEquals("www.test.com:80", parseHostAndPort("www.test.com:80/test"));
        assertEquals("www.test.com", parseHostAndPort("http://www.test.com"));
        assertEquals("www.test.com", parseHostAndPort("www.test.com"));
        assertEquals("httpbin.org:443", parseHostAndPort("httpbin.org:443/get"));
    }
}
