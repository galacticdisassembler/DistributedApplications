package com.ivan.data_warehouse.servlets;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ivan.common.models.ArticleModel;
import com.ivan.data_warehouse.ArticleDAO;
import com.ivan.data_warehouse.InternalSyncMechanism;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;

public class ServiceMechanismServlet extends HttpServlet {

    private static final Logger logger = LogManager.getLogger(ServiceMechanismServlet.class);

    private final InternalSyncMechanism internalSyncMechanism = InternalSyncMechanism.getInstance();
    private final ArticleDAO articleDao = ArticleDAO.getInstance();

    private String getCurrentUsers() {
        return internalSyncMechanism.getUsersCountIsServedNow().toString();
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        try (PrintWriter pw = response.getWriter();) {

            response.setStatus(200);
            pw.print(getCurrentUsers());

        } catch (Exception e) {
            logger.error(e);
        }
    }

    /**
     * Sync this node args: target: http://localhost:3000
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String target = request.getHeader("Target");
        ArticleModel[] articles = new ArticleModel[] {};


        try (PrintWriter printWriter = response.getWriter();
                CloseableHttpClient httpClient = HttpClients.createDefault();) {

            HttpGet req = new HttpGet(target + "/articles?offset=0&limit=9999");
            req.setHeader("Content-Type", "application/json");

            try (CloseableHttpResponse resp = httpClient.execute(req)) {

                HttpEntity entity = resp.getEntity();
                if (entity != null) {
                    String result = EntityUtils.toString(entity);
                    ObjectMapper objectMapper = new ObjectMapper();
                    articles = objectMapper.readValue(result, ArticleModel[].class);

                    int newIdLastValue = Arrays.stream(articles)
                            .max((a, b) -> a.getId() > b.getId() ? 1 : -1)
                            .orElseThrow(() -> new RuntimeException())
                            .getId();

                    if (articleDao.cleanAndInsert(articles, newIdLastValue)) {
                        printWriter.print("1");
                    } else {
                        printWriter.print("0");
                    }

                } else {
                    printWriter.print("0");
                }
            }

        } catch (Exception e) {
            logger.error(e);
        }
    }

}
