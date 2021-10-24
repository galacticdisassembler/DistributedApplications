package com.ivan.data_warehouse.servlets;

import com.ivan.common.models.ArticleModel;
import com.ivan.data_warehouse.InternalSyncMechanism;
import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class ArticlesAsyncServlet extends HttpServlet {

    private InternalSyncMechanism internalSyncMechanism = InternalSyncMechanism.getInstance();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        internalSyncMechanism.getUsersCountIsServedNow().incrementAndGet();

        AsyncContext asyncContext = request.startAsync();
        ServletOutputStream out = response.getOutputStream();
        String contentType = request.getContentType();
        // Map<String, String[]> params = UtilsStaticMethods.getQueryParameters(request);

        ArticleModel model = new ArticleModel();
        model.setAuthorFullName("AuthorFN");
        model.setCategory("category");
        model.setContent("Content ...");
        model.setId(1);
        model.setTitle("Hello");
        List<ArticleModel> list = Arrays.asList(model);

        out.setWriteListener(new CustomAsyncWriteListener(contentType, getServletContext(),
                asyncContext, list, internalSyncMechanism, response));
    }
}
