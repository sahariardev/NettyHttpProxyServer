package org.sahariardev;

import javax.net.ssl.SSLException;

public class Main {
    public static void main(String[] args) throws InterruptedException, SSLException {
        ConfigCron cron = new ConfigCron();
        cron.run();

        HttpProxyServer server = new HttpProxyServer();
        server.start(7001, "localhost");
    }
}