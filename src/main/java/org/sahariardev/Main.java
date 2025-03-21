package org.sahariardev;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) throws InterruptedException {

        HttpProxyServer server = new HttpProxyServer();
        server.start(8080, "localhost", 3000);
    }
}