package io.github.vyo.proxy;

import static org.junit.Assert.*;
import io.github.vyo.proxy.ActivityTracker;
import io.github.vyo.proxy.FlowContext;
import io.github.vyo.proxy.FullFlowContext;
import io.github.vyo.proxy.HttpProxyServer;
import io.github.vyo.proxy.HttpProxyServerBootstrap;
import io.github.vyo.proxy.impl.DefaultHttpProxyServer;
import io.netty.handler.codec.http.HttpRequest;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.cert.X509Certificate;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.util.EntityUtils;
import org.eclipse.jetty.server.Server;
import org.junit.After;
import org.junit.Before;

/**
 * Base for tests that test the proxy. This base class encapsulates all of the
 * testing infrastructure.
 */
public abstract class AbstractProxyTest {
    protected static final String DEFAULT_RESOURCE = "/";

    protected static final AtomicInteger WEB_SERVER_PORT_SEQ = new AtomicInteger(
            50000);
    protected static final AtomicInteger WEB_SERVER_HTTPS_PORT_SEQ = new AtomicInteger(
            53000);
    protected static final AtomicInteger PROXY_SERVER_PORT_SEQ = new AtomicInteger(
            56000);

    protected int webServerPort = 0;
    protected int httpsWebServerPort = 0;
    protected int proxyServerPort = 0;

    protected HttpHost webHost = new HttpHost("127.0.0.1",
            webServerPort);
    protected HttpHost httpsWebHost = new HttpHost(
            "127.0.0.1", httpsWebServerPort, "https");

    /**
     * The server used by the tests.
     */
    protected HttpProxyServer proxyServer;

    /**
     * Holds the most recent response after executing a test method.
     */
    protected String lastResponse;

    /**
     * The web server that provides the back-end.
     */
    private Server webServer;

    protected AtomicInteger bytesReceivedFromClient;
    protected AtomicInteger requestsReceivedFromClient;
    protected AtomicInteger bytesSentToServer;
    protected AtomicInteger requestsSentToServer;
    protected AtomicInteger bytesReceivedFromServer;
    protected AtomicInteger responsesReceivedFromServer;
    protected AtomicInteger bytesSentToClient;
    protected AtomicInteger responsesSentToClient;
    protected AtomicInteger clientConnects;
    protected AtomicInteger clientSSLHandshakeSuccesses;
    protected AtomicInteger clientDisconnects;

    @Before
    public void initializeCounters() {
        bytesReceivedFromClient = new AtomicInteger(0);
        requestsReceivedFromClient = new AtomicInteger(0);
        bytesSentToServer = new AtomicInteger(0);
        requestsSentToServer = new AtomicInteger(0);
        bytesReceivedFromServer = new AtomicInteger(0);
        responsesReceivedFromServer = new AtomicInteger(0);
        bytesSentToClient = new AtomicInteger(0);
        responsesSentToClient = new AtomicInteger(0);
        clientConnects = new AtomicInteger(0);
        clientSSLHandshakeSuccesses = new AtomicInteger(0);
        clientDisconnects = new AtomicInteger(0);
    }

    @Before
    public void runSetUp() throws Exception {
        // Set up new ports for everything based on sequence numbers
        webServerPort = WEB_SERVER_PORT_SEQ.getAndIncrement();
        httpsWebServerPort = WEB_SERVER_HTTPS_PORT_SEQ.getAndIncrement();
        proxyServerPort = PROXY_SERVER_PORT_SEQ.getAndIncrement();

        webHost = new HttpHost("127.0.0.1",
                webServerPort);
        httpsWebHost = new HttpHost(
                "127.0.0.1", httpsWebServerPort, "https");

        webServer = TestUtils.startWebServer(webServerPort,
                httpsWebServerPort);
        setUp();
    }

    protected abstract void setUp() throws Exception;

    @After
    public void runTearDown() throws Exception {
        try {
            tearDown();
        } finally {
            try {
                if (this.proxyServer != null) {
                    this.proxyServer.stop();
                }
            } finally {
                if (this.webServer != null) {
                    webServer.stop();
                }
            }
        }
    }

    protected void tearDown() throws Exception {
    }

    /**
     * Override this to specify a username to use when authenticating with
     * proxy.
     * 
     * @return
     */
    protected String getUsername() {
        return null;
    }

    /**
     * Override this to specify a password to use when authenticating with
     * proxy.
     * 
     * @return
     */
    protected String getPassword() {
        return null;
    }

    protected void assertReceivedBadGateway(ResponseInfo response) {
        assertTrue("Received: " + response, response.getStatusCode() == 502);
    }

    protected ResponseInfo httpPostWithApacheClient(
            HttpHost host, String resourceUrl, boolean isProxied)
            throws Exception {
        String username = getUsername();
        String password = getPassword();
        final DefaultHttpClient httpClient = buildHttpClient();
        try {
            if (isProxied) {
                final HttpHost proxy = new HttpHost("127.0.0.1",
                        proxyServerPort);
                httpClient.getParams().setParameter(
                        ConnRoutePNames.DEFAULT_PROXY, proxy);
                if (username != null && password != null) {
                    httpClient.getCredentialsProvider()
                            .setCredentials(
                                    new AuthScope("127.0.0.1",
                                            proxyServerPort),
                                    new UsernamePasswordCredentials(username,
                                            password));
                }
            }

            final HttpPost request = new HttpPost(resourceUrl);
            request.getParams().setParameter(
                    CoreConnectionPNames.CONNECTION_TIMEOUT, 5000);
            // request.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT,
            // 15000);
            final StringEntity entity = new StringEntity("adsf", "UTF-8");
            entity.setChunked(true);
            request.setEntity(entity);

            final HttpResponse response = httpClient.execute(host, request);
            final HttpEntity resEntity = response.getEntity();
            return new ResponseInfo(response.getStatusLine().getStatusCode(),
                    EntityUtils.toString(resEntity));
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    protected ResponseInfo httpGetWithApacheClient(HttpHost host,
            String resourceUrl, boolean isProxied, boolean callHeadFirst)
            throws Exception {
        String username = getUsername();
        String password = getPassword();
        DefaultHttpClient httpClient = buildHttpClient();
        try {
            if (isProxied) {
                HttpHost proxy = new HttpHost("127.0.0.1", proxyServerPort);
                httpClient.getParams().setParameter(
                        ConnRoutePNames.DEFAULT_PROXY, proxy);
                if (username != null && password != null) {
                    httpClient.getCredentialsProvider()
                            .setCredentials(
                                    new AuthScope("127.0.0.1",
                                            proxyServerPort),
                                    new UsernamePasswordCredentials(username,
                                            password));
                }
            }

            Integer contentLength = null;
            if (callHeadFirst) {
                HttpHead request = new HttpHead(resourceUrl);
                request.getParams().setParameter(
                        CoreConnectionPNames.CONNECTION_TIMEOUT, 5000);
                HttpResponse response = httpClient.execute(host, request);
                contentLength = new Integer(response.getFirstHeader(
                        "Content-Length").getValue());
            }

            HttpGet request = new HttpGet(resourceUrl);
            request.getParams().setParameter(
                    CoreConnectionPNames.CONNECTION_TIMEOUT, 5000);
            // request.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT,
            // 15000);

            HttpResponse response = httpClient.execute(host, request);
            HttpEntity resEntity = response.getEntity();

            if (contentLength != null) {
                assertEquals(
                        "Content-Length from GET should match that from HEAD",
                        contentLength,
                        new Integer(response.getFirstHeader("Content-Length")
                                .getValue()));
            }
            return new ResponseInfo(response.getStatusLine().getStatusCode(),
                    EntityUtils.toString(resEntity));
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    private DefaultHttpClient buildHttpClient() throws Exception {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        SSLSocketFactory sf = new SSLSocketFactory(
                new TrustSelfSignedStrategy(), new X509HostnameVerifier() {
                    public boolean verify(String arg0, SSLSession arg1) {
                        return true;
                    }

                    public void verify(String host, String[] cns,
                            String[] subjectAlts)
                            throws SSLException {
                    }

                    public void verify(String host, X509Certificate cert)
                            throws SSLException {
                    }

                    public void verify(String host, SSLSocket ssl)
                            throws IOException {
                    }
                });
        Scheme scheme = new Scheme("https", 443, sf);
        httpClient.getConnectionManager().getSchemeRegistry().register(scheme);
        return httpClient;
    }

    protected String compareProxiedAndUnproxiedPOST(HttpHost host,
            String resourceUrl) throws Exception {
        ResponseInfo proxiedResponse = httpPostWithApacheClient(host,
                resourceUrl, true);
        if (expectBadGatewayForEverything()) {
            assertReceivedBadGateway(proxiedResponse);
        } else {
            ResponseInfo unproxiedResponse = httpPostWithApacheClient(host,
                    resourceUrl, false);
            assertEquals(unproxiedResponse, proxiedResponse);
            checkStatistics(host);
        }
        return proxiedResponse.getBody();
    }

    protected String compareProxiedAndUnproxiedGET(HttpHost host,
            String resourceUrl) throws Exception {
        ResponseInfo proxiedResponse = httpGetWithApacheClient(host,
                resourceUrl, true, false);
        if (expectBadGatewayForEverything()) {
            assertReceivedBadGateway(proxiedResponse);
        } else {
            ResponseInfo unproxiedResponse = httpGetWithApacheClient(host,
                    resourceUrl, false, false);
            assertEquals(unproxiedResponse, proxiedResponse);
            checkStatistics(host);
        }
        return proxiedResponse.getBody();
    }

    private void checkStatistics(HttpHost host) {
        boolean isHTTPS = host.getSchemeName().equalsIgnoreCase("HTTPS");
        int numberOfExpectedClientInteractions = 1;
        int numberOfExpectedServerInteractions = 1;
        if (isAuthenticating()) {
            numberOfExpectedClientInteractions += 1;
        }
        if (isHTTPS && isMITM()) {
            numberOfExpectedClientInteractions += 1;
            numberOfExpectedServerInteractions += 1;
        }
        if (isHTTPS && !isChained()) {
            numberOfExpectedServerInteractions -= 1;
        }
        assertTrue(bytesReceivedFromClient.get() > 0);
        assertEquals(numberOfExpectedClientInteractions,
                requestsReceivedFromClient.get());
        assertTrue(bytesSentToServer.get() > 0);
        assertEquals(numberOfExpectedServerInteractions,
                requestsSentToServer.get());
        assertTrue(bytesReceivedFromServer.get() > 0);
        assertEquals(numberOfExpectedServerInteractions,
                responsesReceivedFromServer.get());
        assertTrue(bytesSentToClient.get() > 0);
        assertEquals(numberOfExpectedClientInteractions,
                responsesSentToClient.get());
    }

    /**
     * Override this to indicate that the proxy is chained.
     */
    protected boolean isChained() {
        return false;
    }

    /**
     * Override this to indicate that the test uses authentication.
     */
    protected boolean isAuthenticating() {
        return false;
    }

    protected boolean isMITM() {
        return false;
    }

    protected boolean expectBadGatewayForEverything() {
        return false;
    }

    protected HttpProxyServerBootstrap bootstrapProxy() {
        return DefaultHttpProxyServer.bootstrap().plusActivityTracker(
                new ActivityTracker() {
                    @Override
                    public void bytesReceivedFromClient(
                            FlowContext flowContext,
                            int numberOfBytes) {
                        bytesReceivedFromClient.addAndGet(numberOfBytes);
                    }

                    @Override
                    public void requestReceivedFromClient(
                            FlowContext flowContext,
                            HttpRequest httpRequest) {
                        requestsReceivedFromClient.incrementAndGet();
                    }

                    @Override
                    public void bytesSentToServer(FullFlowContext flowContext,
                            int numberOfBytes) {
                        bytesSentToServer.addAndGet(numberOfBytes);
                    }

                    @Override
                    public void requestSentToServer(
                            FullFlowContext flowContext,
                            HttpRequest httpRequest) {
                        requestsSentToServer.incrementAndGet();
                    }

                    @Override
                    public void bytesReceivedFromServer(
                            FullFlowContext flowContext,
                            int numberOfBytes) {
                        bytesReceivedFromServer.addAndGet(numberOfBytes);
                    }

                    @Override
                    public void responseReceivedFromServer(
                            FullFlowContext flowContext,
                            io.netty.handler.codec.http.HttpResponse httpResponse) {
                        responsesReceivedFromServer.incrementAndGet();
                    }

                    @Override
                    public void bytesSentToClient(FlowContext flowContext,
                            int numberOfBytes) {
                        bytesSentToClient.addAndGet(numberOfBytes);
                    }

                    @Override
                    public void responseSentToClient(
                            FlowContext flowContext,
                            io.netty.handler.codec.http.HttpResponse httpResponse) {
                        responsesSentToClient.incrementAndGet();
                    }

                    @Override
                    public void clientConnected(InetSocketAddress clientAddress) {
                        clientConnects.incrementAndGet();
                    }

                    @Override
                    public void clientSSLHandshakeSucceeded(
                            InetSocketAddress clientAddress,
                            SSLSession sslSession) {
                        clientSSLHandshakeSuccesses.incrementAndGet();
                    }

                    @Override
                    public void clientDisconnected(
                            InetSocketAddress clientAddress,
                            SSLSession sslSession) {
                        clientDisconnects.incrementAndGet();
                    }
                });
    }
}