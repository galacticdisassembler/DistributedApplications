package com.ivan.smartproxy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.ivan.common.UtilsStaticMethods;
import com.ivan.common.models.ArticleModel;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javatuples.Pair;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ApiServlet extends HttpServlet {

    private static final Logger logger = LogManager.getLogger(ApiServlet.class);

    private final Map<Integer, ArticleModel> _cache = new HashMap<>();

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String contentType = request.getContentType();
        ArticleModel[] articles = new ArticleModel[] {};
        Map<String, String[]> params = UtilsStaticMethods.getQueryParameters(request);
        boolean needsToSaveInCache = false;

        try (PrintWriter pw = response.getWriter();) {

            if (params.containsKey("id")) {
                int id = Integer.parseInt(params.get("id")[0]);
                if (extractFromCacheWithSuccess(pw, id, contentType)) {
                    return;
                }
                needsToSaveInCache = true;
            }

            Pair<Integer, String> dwHost = Static.SYNC_SERVICE_CONNECTOR_WRAPPER
                    .getHostsWithProcessUsers()
                    .get(0);

            logger.info("Proxy req to DW {} with processing users {}",
                    dwHost.getValue1(), dwHost.getValue0());

            try (CloseableHttpClient httpClient1 = HttpClients.createDefault();) {

                HttpGet req1 =
                        new HttpGet(dwHost.getValue1() + "/articles?" + request.getQueryString());
                req1.setHeader("Content-Type", contentType);

                try (CloseableHttpResponse resp1 = httpClient1.execute(req1)) {

                    HttpEntity entity1 = resp1.getEntity();
                    if (entity1 != null) {
                        String body = EntityUtils.toString(entity1);
                        if (contentType.equals("application/json")) {
                            ObjectMapper objectMapper = new ObjectMapper();
                            articles = objectMapper.readValue(body, ArticleModel[].class);
                        }

                        if (contentType.equals("application/xml")) {
                            ObjectMapper objectMapper = new XmlMapper();
                            articles = objectMapper.readValue(body, ArticleModel[].class);
                        }

                        if (articles.length > 0 && needsToSaveInCache) {
                            for (ArticleModel a : articles) {
                                _cache.put(a.getId(), a);
                            }
                        }

                        response.setHeader("Current-DW", dwHost.getValue1());
                        response.setHeader("All-DWs", proxyInfo());

                        pw.write(body);
                    }
                }

            }

        } catch (Exception e) {
            response.setStatus(400);
            logger.error(e);
        }
    }

    private boolean extractFromCacheWithSuccess(PrintWriter pw, int articleId, String contentType)
            throws Exception {
        String respString = "";
        ArticleModel responseObject = null;

        if (_cache.containsKey(articleId)) {
            responseObject = _cache.get(articleId);
        } else {
            return false;
        }

        if (contentType.equals("application/json")) {
            ObjectMapper objectMapper = new ObjectMapper();
            respString = objectMapper.writeValueAsString(responseObject);
        }

        if (contentType.equals("application/xml")) {
            XmlMapper xmlMapper = new XmlMapper();
            respString = xmlMapper.writeValueAsString(responseObject);
        }

        if (!"".equals(respString)) {
            pw.write(respString);
            return true;
        }

        return false;
    }

    /**
     * Sync this node args: target: http://localhost:3000
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String contentType = request.getContentType();
        String body = UtilsStaticMethods.getBody(request);
        ArticleModel[] articles = new ArticleModel[] {};

        try (PrintWriter pw = response.getWriter();) {
            if (contentType.equals("application/json")) {
                ObjectMapper objectMapper = new ObjectMapper();
                articles = objectMapper.readValue(body, ArticleModel[].class);
            }

            if (contentType.equals("application/xml")) {
                ObjectMapper objectMapper = new XmlMapper();
                articles = objectMapper.readValue(body, ArticleModel[].class);
            }

            Pair<Integer, String> dwHost = Static.SYNC_SERVICE_CONNECTOR_WRAPPER
                    .getHostsWithProcessUsers()
                    .get(0);

            logger.info("Proxy req to DW {} with processing users {}",
                    dwHost.getValue1(), dwHost.getValue0());

            try (CloseableHttpClient httpClient = HttpClients.createDefault();) {
                HttpPost req = new HttpPost(dwHost.getValue1() + "/articles");
                req.setHeader("Content-Type", contentType);
                StringEntity textBody = new StringEntity(body);
                req.setEntity(textBody);

                try (CloseableHttpResponse resp = httpClient.execute(req)) {
                    HttpEntity entity = resp.getEntity();
                    if (entity != null) {
                        String result = EntityUtils.toString(entity);

                        req.setHeader("Current-DW", dwHost.getValue1());
                        req.setHeader("All-DWs", proxyInfo());

                        pw.write(result);
                    }
                }

            } catch (Exception e) {
                logger.error(e);
            }


            response.setStatus(200);
        } catch (Exception e) {
            response.setStatus(400);
            logger.error(e);
        }
    }


    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String contentType = request.getContentType();
        String body = UtilsStaticMethods.getBody(request);

        ArticleModel model = new ArticleModel();

        try (PrintWriter pw = response.getWriter();) {
            if (contentType.equals("application/json")) {
                ObjectMapper objectMapper = new ObjectMapper();
                model = objectMapper.readValue(body, ArticleModel.class);
            }

            if (contentType.equals("application/xml")) {
                ObjectMapper objectMapper = new XmlMapper();
                model = objectMapper.readValue(body, ArticleModel.class);
            }

            Pair<Integer, String> dwHost = Static.SYNC_SERVICE_CONNECTOR_WRAPPER
                    .getHostsWithProcessUsers()
                    .get(0);

            logger.info("Proxy req to DW {} with processing users {}",
                    dwHost.getValue1(), dwHost.getValue0());

            try (CloseableHttpClient httpClient = HttpClients.createDefault();) {
                HttpPut req = new HttpPut(dwHost.getValue1() + "/articles");
                req.setHeader("Content-Type", contentType);
                StringEntity textBody = new StringEntity(body);
                req.setEntity(textBody);

                try (CloseableHttpResponse resp = httpClient.execute(req)) {
                    HttpEntity entity = resp.getEntity();
                    if (entity != null) {
                        String result = EntityUtils.toString(entity);

                        if (result.equals("SUCCESS")) {
                            _cache.put(model.getId(), model);
                        }

                        req.setHeader("Current-DW", dwHost.getValue1());
                        req.setHeader("All-DWs", proxyInfo());
                        pw.write(result);
                    }
                }

            }


        } catch (Exception e) {
            response.setStatus(400);
            logger.error(e);
        }
    }

    private String proxyInfo() {

        List<String> arr = Static.SYNC_SERVICE_CONNECTOR_WRAPPER
                .getHostsWithProcessUsers()
                .stream()
                .map(a -> String.format("%s(%d)", a.getValue1(), a.getValue0()))
                .collect(Collectors.toList());

        StringBuilder result = new StringBuilder();

        for (String s : arr) {
            result.append(s + ';');
        }


        return result.toString();
    }


}
