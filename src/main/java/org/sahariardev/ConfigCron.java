package org.sahariardev;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author sahariar.alam
 * @since 7/31/25 11:03AM
 */
public class ConfigCron {

    public void run() {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        Gson gson = new Gson();
        String connectionUrl = "http://localhost:7002/api/deployments";

        Runnable task = () -> {
            System.out.println("Running Cron Task at " + LocalDateTime.now());
            try {
                URL url = new URL(connectionUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                InputStreamReader reader = new InputStreamReader(connection.getInputStream());
                Type listType = new TypeToken<List<Deployment>>() {
                }.getType();
                List<Deployment> deployments = gson.fromJson(reader, listType);
                reader.close();
                connection.disconnect();

                for (Deployment deployment : deployments) {
                    System.out.println(deployment);
                }

                Store.setDeployments(deployments);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };

        executor.scheduleAtFixedRate(task, 0, 1, TimeUnit.MINUTES);
    }
}
