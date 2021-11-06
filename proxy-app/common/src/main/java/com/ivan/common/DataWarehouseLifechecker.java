package com.ivan.common;

import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javatuples.Pair;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class DataWarehouseLifechecker {
    private static final Logger logger = LogManager.getLogger(DataWarehouseLifechecker.class);

    private final Set<String> dataWarehouses;
    private Thread loopThread;
    private boolean running;
    private ExecutorService executorService = Executors.newCachedThreadPool();

    public DataWarehouseLifechecker(Set<String> dataWarehouses) {
        this.dataWarehouses = dataWarehouses;
        this.running = false;
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
                List<Pair<Future<Boolean>, String>> pairs = new LinkedList<>();
                for (String dw : dataWarehouses) {
                    Future<Boolean> f = executorService.submit(() -> {
                        return dataWarehouseIsAlive(dw);
                    });

                    pairs.add(Pair.with(f, dw));
                }

                Thread.sleep(1000);
                StringJoiner synchronizedDataWarehouses = new StringJoiner(";");

                for (Pair<Future<Boolean>, String> p : pairs) {
                    boolean removed = false;

                    if (!p.getValue0().isDone() || p.getValue0().isCancelled()) {
                        dataWarehouses.remove(p.getValue1());
                        removed = true;
                    } else {
                        try {
                            if (!p.getValue0().get()) {
                                dataWarehouses.remove(p.getValue1());
                                removed = true;
                            }
                        } catch (InterruptedException | ExecutionException e) {
                            dataWarehouses.remove(p.getValue1());
                            removed = true;
                            logger.error(e);
                        }
                    }

                    if (removed) {
                        logger.info(String.format("DW host from SyncService %s removed",
                                p.getValue1()));
                    } else {
                        synchronizedDataWarehouses.add(p.getValue1());
                    }

                }

                logger.info("Synchronized DW\'s: {}", synchronizedDataWarehouses);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean dataWarehouseIsAlive(String dwHost) {

        int timeoutImMillis = 700;
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(timeoutImMillis)
                .setConnectionRequestTimeout(timeoutImMillis)
                .setSocketTimeout(timeoutImMillis).build();


        try (CloseableHttpClient httpClient =
                HttpClientBuilder.create().setDefaultRequestConfig(config).build()) {

            HttpGet req = new HttpGet(
                    String.format("%s/service", dwHost));

            try (CloseableHttpResponse resp = httpClient.execute(req)) {

                HttpEntity entity = resp.getEntity();
                if (entity != null) {
                    boolean isAlive = resp.getStatusLine().getStatusCode() == 200;
                    return isAlive;
                }
            }

            req.releaseConnection();
        } catch (Exception e) {
            logger.error(e);
        }

        return false;
    }
}
