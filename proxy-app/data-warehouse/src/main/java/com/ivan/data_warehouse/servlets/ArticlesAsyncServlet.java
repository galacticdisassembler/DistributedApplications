package com.ivan.data_warehouse.servlets;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.ivan.common.UtilsStaticMethods;
import com.ivan.common.models.ArticleModel;
import com.ivan.data_warehouse.ArticleDAO;
import com.ivan.data_warehouse.InternalSyncMechanism;
import com.ivan.data_warehouse.SyncServiceConnector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.http.HttpStatus;
import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;

public class ArticlesAsyncServlet extends HttpServlet {
    private static final Logger logger = LogManager.getLogger(ArticlesAsyncServlet.class);

    private static final String TEXT_PLAIN_RESPONSE_CONTENT_TYPE = "text/plain";

    private final InternalSyncMechanism internalSyncMechanism;
    private final ArticleDAO articleDao;
    private final SyncServiceConnector syncServiceConnector;

    public ArticlesAsyncServlet() {
        this.internalSyncMechanism = InternalSyncMechanism.getInstance();
        this.articleDao = ArticleDAO.getInstance();
        this.syncServiceConnector = SyncServiceConnector.getInstance();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        AsyncContext asyncContext = request.startAsync();
        ServletOutputStream out = response.getOutputStream();
        String contentType = request.getContentType();
        CustomAsyncWriteListener asyncRunner =
                buildCustomAsyncWriteListenerForErrorCase(asyncContext, response);

        List<ArticleModel> resultList = new ArrayList<>();

        try {
            internalSyncMechanism.getUsersCountIsServedNow().incrementAndGet();

            Map<String, String[]> params = UtilsStaticMethods.getQueryParameters(request);

            if (params.containsKey("id")) {
                int id = Integer.parseInt(params.get("id")[0]);
                Optional<ArticleModel> articleFromDb = articleDao.select(id);
                if (articleFromDb.isPresent()) {
                    resultList = Arrays.asList(articleFromDb.get());
                } else {
                    resultList = new ArrayList<>();
                }
            } else if (params.containsKey("limit") && params.containsKey("offset")) {
                int limit = Integer.parseInt(params.get("limit")[0]);
                int offset = Integer.parseInt(params.get("offset")[0]);
                resultList = articleDao.select(offset, limit);
            }

            asyncRunner = new CustomAsyncWriteListener(
                    contentType, getServletContext(),
                    asyncContext, resultList, HttpStatus.OK_200,
                    internalSyncMechanism, response);
        } catch (Exception e) {
            logger.error(e);

        } finally {
            out.setWriteListener(asyncRunner);
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        AsyncContext asyncContext = request.startAsync();
        CustomAsyncWriteListener asyncRunner =
                buildCustomAsyncWriteListenerForErrorCase(asyncContext, response);
        ServletOutputStream out = response.getOutputStream();
        String contentType = request.getContentType();
        String body = UtilsStaticMethods.getBody(request);

        ArticleModel model = new ArticleModel();

        try {
            internalSyncMechanism.getUsersCountIsServedNow().incrementAndGet();

            if (contentType.equals("application/json")) {
                ObjectMapper objectMapper = new ObjectMapper();
                model = objectMapper.readValue(body, ArticleModel.class);
            }

            if (contentType.equals("application/xml")) {
                ObjectMapper objectMapper = new XmlMapper();
                model = objectMapper.readValue(body, ArticleModel.class);
            }

            boolean updatedWithSuccess = articleDao
                    .update(model);

            if (updatedWithSuccess) {

                syncServiceConnector.syncAllDWsWithMe();
                asyncRunner = new CustomAsyncWriteListener(
                        TEXT_PLAIN_RESPONSE_CONTENT_TYPE, getServletContext(),
                        asyncContext, "SUCCESS", HttpStatus.CREATED_201,
                        internalSyncMechanism, response);
            }

        } catch (Exception e) {
            logger.error(e);
        } finally {
            out.setWriteListener(asyncRunner);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        ServletOutputStream out = response.getOutputStream();
        String contentType = request.getContentType();
        String body = UtilsStaticMethods.getBody(request);
        AsyncContext asyncContext = request.startAsync();
        ArticleModel[] articles = new ArticleModel[] {};
        CustomAsyncWriteListener asyncRunner =
                buildCustomAsyncWriteListenerForErrorCase(asyncContext, response);


        try {
            internalSyncMechanism.getUsersCountIsServedNow().incrementAndGet();

            if (contentType.equals("application/json")) {
                ObjectMapper objectMapper = new ObjectMapper();
                articles = objectMapper.readValue(body, ArticleModel[].class);
            }

            if (contentType.equals("application/xml")) {
                ObjectMapper objectMapper = new XmlMapper();
                articles = objectMapper.readValue(body, ArticleModel[].class);
            }

            boolean insertedWithSuccess = articleDao.insert(articles);

            if (insertedWithSuccess) {
                StringJoiner joiner = new StringJoiner(",");
                for (ArticleModel a : articles) {
                    joiner.add(a.getId().toString());
                }

                syncServiceConnector.syncAllDWsWithMe();
                asyncRunner = new CustomAsyncWriteListener(
                        TEXT_PLAIN_RESPONSE_CONTENT_TYPE, getServletContext(),
                        asyncContext, joiner.toString(), HttpStatus.CREATED_201,
                        internalSyncMechanism, response);
            }
        } catch (Exception e) {
            logger.error(e);
        } finally {
            out.setWriteListener(asyncRunner);
        }
    }

    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        AsyncContext asyncContext = request.startAsync();
        ServletOutputStream out = response.getOutputStream();
        CustomAsyncWriteListener asyncRunner =
                buildCustomAsyncWriteListenerForErrorCase(asyncContext, response);

        try {
            internalSyncMechanism.getUsersCountIsServedNow().incrementAndGet();

            Map<String, String[]> params = UtilsStaticMethods.getQueryParameters(request);
            int delayInMilliseconds = 0;

            if (params.containsKey("delayInMilliseconds")) {
                delayInMilliseconds = Integer.parseInt(params.get("delayInMilliseconds")[0]);
            }

            response.addHeader("Allow", "OPTIONS, GET, HEAD, POST, PUT, DELETE");

            asyncRunner = new CustomAsyncWriteListener(
                    TEXT_PLAIN_RESPONSE_CONTENT_TYPE, getServletContext(),
                    asyncContext, "", HttpStatus.NO_CONTENT_204,
                    internalSyncMechanism, response);

            asyncRunner.setDevOnlyResponseDelayMilliseconds(delayInMilliseconds);
        } catch (Exception e) {
            logger.error(e);

        } finally {
            out.setWriteListener(asyncRunner);
        }
    }

    @Override
    protected void doHead(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        AsyncContext asyncContext = request.startAsync();
        ServletOutputStream out = response.getOutputStream();
        CustomAsyncWriteListener asyncRunner =
                buildCustomAsyncWriteListenerForErrorCase(asyncContext, response);

        try {
            internalSyncMechanism.getUsersCountIsServedNow().incrementAndGet();

            asyncRunner = new CustomAsyncWriteListener(
                    TEXT_PLAIN_RESPONSE_CONTENT_TYPE, getServletContext(),
                    asyncContext, "", HttpStatus.NO_CONTENT_204,
                    internalSyncMechanism, response);
        } catch (Exception e) {
            logger.error(e);

        } finally {
            out.setWriteListener(asyncRunner);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        AsyncContext asyncContext = request.startAsync();
        ServletOutputStream out = response.getOutputStream();
        CustomAsyncWriteListener asyncRunner =
                buildCustomAsyncWriteListenerForErrorCase(asyncContext, response);
        String body = UtilsStaticMethods.getBody(request);


        try {
            internalSyncMechanism.getUsersCountIsServedNow().incrementAndGet();
            int id = Integer.parseInt(body);
            if (articleDao.delete(id)) {
                asyncRunner = new CustomAsyncWriteListener(
                        TEXT_PLAIN_RESPONSE_CONTENT_TYPE, getServletContext(),
                        asyncContext, "", HttpStatus.CREATED_201,
                        internalSyncMechanism, response);
            }
        } catch (Exception e) {
            logger.error(e);

        } finally {
            out.setWriteListener(asyncRunner);
        }
    }

    private CustomAsyncWriteListener buildCustomAsyncWriteListenerForErrorCase(
            AsyncContext asyncContext, HttpServletResponse response) {
        return new CustomAsyncWriteListener(
                TEXT_PLAIN_RESPONSE_CONTENT_TYPE, getServletContext(),
                asyncContext, "Error :(", HttpStatus.EXPECTATION_FAILED_417,
                internalSyncMechanism, response);
    }

}
