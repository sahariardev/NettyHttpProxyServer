package org.sahariardev;

import java.util.ArrayList;
import java.util.List;

/**
 * @author sahariar.alam
 * @since 7/31/25 11:37 AM
 */
public class Store {

    private static List<Deployment> deployments = new ArrayList<>();

    public static List<Deployment> getDeployments() {
        return deployments;
    }

    public static synchronized void setDeployments(List<Deployment> deployments) {
        Store.deployments = deployments;
    }
}
