package com.ivan.data_warehouse.servlets;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.ivan.common.models.ArticleModel;
import com.ivan.data_warehouse.InternalSyncMechanism;
import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ArticlesAsyncServlet extends HttpServlet {
    private static String HEAVY_RESOURCE =
            "This is some heavy resource that will be served in an async way";

    private InternalSyncMechanism internalSyncMechanism = InternalSyncMechanism.getInstance();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        internalSyncMechanism.getUsersCountIsServedNow().incrementAndGet();

        AsyncContext async = request.startAsync();
        ServletOutputStream out = response.getOutputStream();
        String contentType = request.getContentType();
        Map<String, String[]> params = UtilsStaticMethods.getQueryParameters(request);

        ArticleModel model = new ArticleModel();
        model.setAuthorFullName("AuthorFN");
        model.setCategory("category");
        model.setContent("Content ...");
        model.setId(1);
        model.setTitle("Hello");
        List<ArticleModel> list = Arrays.asList(model);

        out.setWriteListener(new WriteListener() {
            @Override
            public void onWritePossible() throws IOException {
                String respString = "";

                if (contentType.equals("application/json")) {
                    ObjectMapper objectMapper = new ObjectMapper();
                    respString = objectMapper.writeValueAsString(list);
                }

                if (contentType.equals("application/xml")) {
                    XmlMapper xmlMapper = new XmlMapper();
                    respString = xmlMapper.writeValueAsString(list);
                }

                ByteBuffer content = ByteBuffer.wrap(respString.getBytes(StandardCharsets.UTF_8));


                while (out.isReady()) {
                    if (!content.hasRemaining()) {
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                        }
                        response.setStatus(200);
                        async.complete();
                        internalSyncMechanism
                                .getUsersCountIsServedNow()
                                .decrementAndGet();
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
}
