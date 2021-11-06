package com.ivan.data_warehouse;

import com.ivan.data_warehouse.servlets.ArticlesAsyncServlet;
import com.ivan.data_warehouse.servlets.ServiceMechanismServlet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;

public class JettyHttpServer {

    private static final Logger logger = LogManager.getLogger(JettyHttpServer.class);

    private final SyncServiceConnector syncServiceConnector = SyncServiceConnector.getInstance();

    private Server server;

    public void start(int port, String syncServiceHost) throws Exception {
        if (!syncServiceConnector
                .connectToSyncService(syncServiceHost,
                        "http://localhost:" + Integer.toString(port))) {
            logger.error("Can\'t connect to SyncService!!!");
            System.exit(-1);
        }

        server = new Server();
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(port);
        server.setConnectors(new Connector[] {connector});
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        context.addServlet(ArticlesAsyncServlet.class, "/articles");
        context.addServlet(ServiceMechanismServlet.class, "/service");
        server.setHandler(context);
        logger.info("App is running on port: {}", port);
        server.start();
        server.join();

    }
}
