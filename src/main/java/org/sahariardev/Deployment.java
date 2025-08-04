package org.sahariardev;

/**
 * @author sahariar.alam
 * @since 7/31/25 11:10AM
 */
public class Deployment {
    public String name;

    public int port;

    public String status;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "Deployment{" +
                "name='" + name + '\'' +
                ", port=" + port +
                ", status='" + status + '\'' +
                '}';
    }
}
