package com.ivan.data_warehouse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;

public class JettyHttpServer {

    private static final Logger logger = LogManager.getLogger(JettyHttpServer.class);

    private Server server;

    public void start(int port) throws Exception {
        server = new Server();
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(port);
        server.setConnectors(new Connector[] { connector });
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        context.addServlet(AsyncServlet.class, "/");
        server.setHandler(context);
        logger.info("App is running on port: {}", port);
        server.start();
        server.join();

    }
}
