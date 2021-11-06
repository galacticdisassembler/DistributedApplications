package com.ivan.data_warehouse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ivan.common.models.ArticleModel;
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
import java.util.Arrays;
import java.util.stream.Stream;

public class SyncServiceConnector {
    private static final Logger logger = LogManager.getLogger(SyncServiceConnector.class);

    private static final SyncServiceConnector INSTANCE = new SyncServiceConnector();
    private String currentHost;
    private String syncServiceHost;

    public static SyncServiceConnector getInstance() {
        return INSTANCE;
    }

    private SyncServiceConnector() {

    }


    public boolean connectToSyncService(String syncServiceHost, String currentHost) {
        this.currentHost = currentHost;
        this.syncServiceHost = syncServiceHost;

        int timeout = 7;
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(timeout * 1000)
                .setConnectionRequestTimeout(timeout * 1000)
                .setSocketTimeout(timeout * 1000).build();

        try (CloseableHttpClient httpClient =
                HttpClientBuilder.create().setDefaultRequestConfig(config).build()) {

            HttpGet req = new HttpGet(
                    String.format("%s/sync?newHost=%s", syncServiceHost, currentHost));
            req.setHeader("Content-Type", "application/json");

            try (CloseableHttpResponse resp = httpClient.execute(req)) {

                HttpEntity entity = resp.getEntity();
                if (entity != null) {
                    String result = EntityUtils.toString(entity);
                    if ("1".equals(result)) {
                        return true;
                    }

                    if (result.startsWith("http:")) {
                        boolean resultVal = syncWithDWService(result);
                        return resultVal;
                    }
                }
            }
            req.releaseConnection();
        } catch (Exception e) {
            logger.error(e);
        }

        return false;
    }

    private boolean syncWithDWService(String target) {
        ArticleModel[] articles = new ArticleModel[] {};

        try (CloseableHttpClient httpClient1 = HttpClients.createDefault();) {

            HttpGet req1 = new HttpGet(target + "/articles?offset=0&limit=9999");
            req1.setHeader("Content-Type", "application/json");

            try (CloseableHttpResponse resp1 = httpClient1.execute(req1)) {

                HttpEntity entity1 = resp1.getEntity();
                if (entity1 != null) {
                    String result1 = EntityUtils.toString(entity1);
                    ObjectMapper objectMapper = new ObjectMapper();
                    articles = objectMapper.readValue(result1, ArticleModel[].class);

                    if (articles.length == 0) {
                        return true; //It means that DW is empty 
                    }

                    Stream<ArticleModel> articlesStream = Arrays.stream(articles);

                    int newIdLastValue = articlesStream
                            .max((a, b) -> a.getId() > b.getId() ? 1 : -1)
                            .orElseThrow(() -> new RuntimeException())
                            .getId();

                    if (ArticleDAO.getInstance().cleanAndInsert(articles,
                            newIdLastValue)) {
                        return true;
                    }
                }
            }

        } catch (Exception e) {
            logger.error(e);
        }

        return false;
    }

    public void syncAllDWsWithMe() {
        try (CloseableHttpClient httpClient1 = HttpClients.createDefault();) {

            HttpGet req1 = new HttpGet(syncServiceHost + "/sync?sync=1&origin=" + currentHost);
            req1.setHeader("Content-Type", "application/json");

            try (CloseableHttpResponse resp1 = httpClient1.execute(req1)) {

                HttpEntity entity1 = resp1.getEntity();
                if (entity1 != null && resp1.getStatusLine().getStatusCode() == 200) {
                    String result1 = EntityUtils.toString(entity1);
                    logger.info("{}", result1);
                }
            }

        } catch (Exception e) {
            logger.error(e);
        }
    }

}
