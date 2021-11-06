package com.ivan.sync_service;

import com.ivan.common.UtilsStaticMethods;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

public class SyncServlet extends HttpServlet {
    private static final Logger logger = LogManager.getLogger(SyncServlet.class);

    private final Set<String> hosts = DataWarehouseLifecheckerWrapper.HOSTS;

    public SyncServlet() {}

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Map<String, String[]> params = UtilsStaticMethods.getQueryParameters(request);
        String responseBody = "0";

        if (params.containsKey("newHost")) {
            String connectedHost = params.get("newHost")[0];
            boolean differentHostExists = hosts.size() > 0;
            hosts.add(connectedHost);

            logger.info("Connected new DW with host: {}", connectedHost);

            if (differentHostExists) {
                responseBody = hosts.iterator().next();
            } else {
                responseBody = "1";
            }

        } else if (params.containsKey("sync")) {
            String origin = params.get("origin")[0];

            logger.info("Sync DWs: {}, origin: {}", hosts, origin);

            List<String> synchronizedDWs = syncAllDWServices(origin);
            StringJoiner stringJoiner = new StringJoiner(";");
            for (String s : synchronizedDWs) {
                stringJoiner.add(s);
            }

            logger.info("Sync SUCCESS DWs: {}", stringJoiner.toString());

            responseBody = stringJoiner.toString();
        }

        try (PrintWriter printWriter = response.getWriter();) {
            printWriter.print(responseBody);
        }
    }

    private boolean syncOneDWService(String origin, String host) {

        try (CloseableHttpClient httpClient = HttpClients.createDefault();) {
            HttpPost req = new HttpPost(host + "/service");
            req.setHeader("Target", origin);
            req.setHeader("Content-Type", "application/json");

            try (CloseableHttpResponse resp = httpClient.execute(req)) {
                HttpEntity entity = resp.getEntity();
                if (entity != null) {
                    String result = EntityUtils.toString(entity);
                    if ("1".equals(result)) {
                        return true;
                    }
                }
            }

        } catch (Exception e) {
            logger.error(e);
        }

        return false;
    }

    private List<String> syncAllDWServices(String originHost) {
        List<String> synchronizedDWs = new ArrayList<>();

        for (String h : hosts) {
            if (h.equals(originHost)) {
                continue;
            }

            if (syncOneDWService(originHost, h)) {
                synchronizedDWs.add(h);
            }
        }

        return synchronizedDWs;
    }

}
