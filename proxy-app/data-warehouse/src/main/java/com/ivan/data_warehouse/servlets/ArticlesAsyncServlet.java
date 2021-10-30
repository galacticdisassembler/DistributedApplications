package com.ivan.data_warehouse.servlets;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.ivan.common.models.ArticleModel;
import com.ivan.data_warehouse.ArticleDAO;
import com.ivan.data_warehouse.InternalSyncMechanism;
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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

public class ArticlesAsyncServlet extends HttpServlet {
    private static final Logger logger = LogManager.getLogger(ArticlesAsyncServlet.class);

    public ArticlesAsyncServlet() {
        this.internalSyncMechanism = InternalSyncMechanism.getInstance();
        this.articleDao = ArticleDAO.getInstance();
    }

    private final InternalSyncMechanism internalSyncMechanism;
    private final ArticleDAO articleDao;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        CustomAsyncWriteListener asyncRunner = null;;
        AsyncContext asyncContext = request.startAsync();
        ServletOutputStream out = response.getOutputStream();
        String contentType = request.getContentType();

        ArticleModel model = new ArticleModel();
        List<ArticleModel> resultList = Arrays.asList(model);

        try {
            internalSyncMechanism.getUsersCountIsServedNow().incrementAndGet();

            Map<String, String[]> params = UtilsStaticMethods.getQueryParameters(request);

            if (params.containsKey("id")) {
                int id = Integer.parseInt(params.get("id")[0]);
                resultList = Arrays.asList(articleDao.select(id));
            }

            if (params.containsKey("limit") && params.containsKey("offset")) {
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
            asyncRunner = buildCustomAsyncWriteListenerForErrorCase(asyncContext, response);

        } finally {
            out.setWriteListener(asyncRunner);
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        AsyncContext asyncContext = request.startAsync();
        ServletOutputStream out = response.getOutputStream();
        String contentType = request.getContentType();

        String body = UtilsStaticMethods.getBody(request);

        ArticleModel[] articles = null;
        CustomAsyncWriteListener asyncRunner;

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

            asyncRunner = new CustomAsyncWriteListener(
                    "text/plain", getServletContext(),
                    asyncContext, joiner.toString(), HttpStatus.CREATED_201,
                    internalSyncMechanism, response);
        } else {
            asyncRunner = buildCustomAsyncWriteListenerForErrorCase(asyncContext, response);
        }

        out.setWriteListener(asyncRunner);
    }

    private CustomAsyncWriteListener buildCustomAsyncWriteListenerForErrorCase(
            AsyncContext asyncContext, HttpServletResponse response) {
        return new CustomAsyncWriteListener(
                "text/plain", getServletContext(),
                asyncContext, "Error :(", HttpStatus.EXPECTATION_FAILED_417,
                internalSyncMechanism, response);
    }

}
