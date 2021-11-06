package com.ivan.smartproxy;

import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javatuples.Pair;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SyncServiceConnectorWrapper {
    private static final Logger logger = LogManager.getLogger(SyncServiceConnectorWrapper.class);

    private final List<Pair<Integer, String>> hostsWithProcessUsers;

    private Thread loopThread;
    private boolean running;

    public SyncServiceConnectorWrapper() {
        this.hostsWithProcessUsers = Collections.synchronizedList(new ArrayList<>());
        this.running = false;
    }

    public List<Pair<Integer, String>> getHostsWithProcessUsers() {
        return hostsWithProcessUsers;
    }

    public void run() {
        this.running = true;
        this.loopThread = new Thread(this::loopMethod);
        this.loopThread.run();
    }

    public void stop() {
        this.running = false;
    }

    private void loopMethod() {


        while (running) {
            try {

                try (CloseableHttpClient httpClient = HttpClients.createDefault();) {
                    HttpGet req = new HttpGet(Static.syncServiceHost + "/sync?getAllDWs=1");

                    try (CloseableHttpResponse resp = httpClient.execute(req)) {
                        HttpEntity entity = resp.getEntity();
                        if (entity != null) {
                            String result = EntityUtils.toString(entity);
                            if (result != null) {
                                String[] dws = result.split(";");
                                processDWs(dws);
                            }

                            // logger.info(result);
                        }
                    }

                } catch (Exception e) {
                    logger.error(e);
                }

                Thread.sleep(500);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void processDWs(String[] dws) {
        hostsWithProcessUsers.clear();

        for (String host : dws) {
            int timeoutImMillis = 700;
            RequestConfig config = RequestConfig.custom()
                    .setConnectTimeout(timeoutImMillis)
                    .setConnectionRequestTimeout(timeoutImMillis)
                    .setSocketTimeout(timeoutImMillis).build();


            try (CloseableHttpClient httpClient =
                    HttpClientBuilder.create().setDefaultRequestConfig(config).build()) {

                HttpGet req = new HttpGet(
                        String.format("%s/service", host));

                try (CloseableHttpResponse resp = httpClient.execute(req)) {

                    HttpEntity entity = resp.getEntity();
                    if (entity != null) {
                        boolean isAlive = resp.getStatusLine().getStatusCode() == 200;
                        if (isAlive) {
                            String result = EntityUtils.toString(entity);

                            try {
                                Integer userProcessing = Integer.parseInt(result);
                                hostsWithProcessUsers.add(Pair.with(userProcessing, host));
                            } catch (Exception e) {

                            }

                            logger.info("DW({}) - processing users {}", host, result);
                        }
                    }
                }

                req.releaseConnection();
            } catch (Exception e) {
                logger.error(e);
            }
        }

        hostsWithProcessUsers.sort((a, b) -> a.getValue0() > b.getValue0() ? 1 : -1);
    }

}
