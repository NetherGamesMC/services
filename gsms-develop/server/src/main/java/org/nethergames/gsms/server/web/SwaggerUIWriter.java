package org.nethergames.gsms.server.web;

import io.sentry.Sentry;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class SwaggerUIWriter {
    public static void readAndWrite(int port) {
        try {
            URL url = new URL("http://localhost:" + port + "/swagger-docs");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            connection.connect();

            BufferedReader response = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder builder = new StringBuilder();
            String data;
            while ((data = response.readLine()) != null) {
                builder.append(data);
            }

            response.close();
            connection.disconnect();
            String root = System.getProperty("user.dir");
            File file = new File(root + "/swagger-docs.json");
            System.out.println("dumping to " + root + "/swagger-docs.json");
            FileOutputStream stream = new FileOutputStream(file);
            stream.write(builder.toString().getBytes(StandardCharsets.UTF_8));

            stream.close();
        } catch (Throwable t) {
            Sentry.captureException(t);
            System.err.println(t.getMessage());
        }

    }
}
