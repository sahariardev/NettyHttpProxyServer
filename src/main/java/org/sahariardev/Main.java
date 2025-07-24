package org.sahariardev;

import javax.net.ssl.SSLException;

public class Main {
    public static void main(String[] args) throws InterruptedException, SSLException {

        HttpProxyServer server = new HttpProxyServer();
        server.start(4444, "localhost", 4443);
    }
}