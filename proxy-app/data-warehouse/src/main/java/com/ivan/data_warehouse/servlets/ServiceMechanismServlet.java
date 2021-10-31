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
import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class ServiceMechanismServlet extends HttpServlet {

    private InternalSyncMechanism internalSyncMechanism = InternalSyncMechanism.getInstance();
    private static final Logger logger = LogManager.getLogger(ServiceMechanismServlet.class);

    private final ArticleDAO articleDao = ArticleDAO.getInstance();

    private String getCurrentUsers() {
        return internalSyncMechanism.getUsersCountIsServedNow().toString();
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        internalSyncMechanism.getUsersCountIsServedNow().incrementAndGet();

        AsyncContext async = request.startAsync();
        ServletOutputStream out = response.getOutputStream();

        out.setWriteListener(new WriteListener() {
            @Override
            public void onWritePossible() throws IOException {
                while (out.isReady()) {
                    ByteBuffer content =
                            ByteBuffer.wrap(getCurrentUsers().getBytes(StandardCharsets.UTF_8));

                    if (!content.hasRemaining()) {
                        response.setStatus(200);
                        async.complete();
                        internalSyncMechanism.getUsersCountIsServedNow().decrementAndGet();
                        return;
                    }
                    out.write(content.get());
                }
            }

            @Override
            public void onError(Throwable t) {
                getServletContext().log("Async Error", t);
                async.complete();
                internalSyncMechanism.getUsersCountIsServedNow().decrementAndGet();
            }
        });
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
            internalSyncMechanism.getUsersCountIsServedNow().incrementAndGet();

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
        } finally {
            internalSyncMechanism.getUsersCountIsServedNow().decrementAndGet();
        }
    }

}
