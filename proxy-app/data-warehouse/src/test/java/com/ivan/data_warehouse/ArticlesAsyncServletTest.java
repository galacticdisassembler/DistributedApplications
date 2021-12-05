package com.ivan.data_warehouse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.ivan.common.models.ArticleModel;
import com.ivan.data_warehouse.servlets.ArticlesAsyncServlet;
import com.ivan.data_warehouse.servlets.ServiceMechanismServlet;
import junit.framework.Assert;
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
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ArticlesAsyncServletTest {

    private static Server server;
    private static final Logger logger = LogManager.getLogger(ArticlesAsyncServletTest.class);
    private static final int PORT = 8080;
    private static final String HOST = "http://localhost:".concat(Integer.toString(PORT));

    @BeforeClass
    public static void setup() throws Exception {

        server = new Server();
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(8080);
        server.setConnectors(new Connector[] {connector});
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        context.addServlet(ArticlesAsyncServlet.class, "/articles");
        context.addServlet(ServiceMechanismServlet.class, "/service");
        server.setHandler(context);
        logger.info("App is running on port: {}", 8080);
        server.start();
    }

    @AfterClass
    public static void end() throws Exception {
        server.stop();
    }

    @Test
    public void perform1PostRequestToDWUsingXMLTest() {

        String body =
                "<ArrayList>" +
                        "    <item>" +
                        "        <authorFullName>ZZZZZZZZAuthorFN22</authorFullName>" +
                        "        <title>Hello</title>" +
                        "        <content>Content ...</content>" +
                        "        <category>category</category>" +
                        "    </item>" +
                        "    <item>" +
                        "        <authorFullName>AuthorFN33</authorFullName>" +
                        "        <title>Hello</title>" +
                        "        <content>Content ...</content>" +
                        "        <category>category</category>" +
                        "    </item>" +
                        "    <item>" +
                        "        <authorFullName>AuthorFN44</authorFullName>" +
                        "        <title>Hello</title>" +
                        "        <content>Content ...</content>" +
                        "        <category>category</category>" +
                        "    </item>" +
                        "</ArrayList>";

        try (CloseableHttpClient httpClient = HttpClients.createDefault();) {
            HttpPost req = new HttpPost(HOST + "/articles");
            req.setHeader("Content-Type", "application/xml");
            StringEntity textBody = new StringEntity(body);
            req.setEntity(textBody);

            try (CloseableHttpResponse resp = httpClient.execute(req)) {
                HttpEntity entity = resp.getEntity();
                if (entity != null) {
                    String result = EntityUtils.toString(entity);
                    int statusCode = resp.getStatusLine().getStatusCode();

                    Assert.assertEquals(5, result.length());
                    Assert.assertEquals(201, statusCode);
                }
            }

        } catch (Exception e) {
            logger.error(e);
            Assert.fail();
        }

    }

    @Test
    public void perform2PostRequestToDWUsingJSONTest() {

        String body =
                "["
                        + "{"
                        + "\"authorFullName\":\"AuthorFN1\","
                        + "\"title\":\"Hello134134\","
                        + "\"content\":\"Content134134 ...\","
                        + "\"category\":\"category134\""
                        + "},"
                        + " {"
                        + "  \"authorFullName\":\"AuthorFN2\","
                        + "  \"title\":\"Hello\","
                        + "  \"content\":\"Contentdafadfdaf ...\","
                        + "  \"category\":\"categorydafadfaddaf\""
                        + " },"
                        + " {"
                        + "  \"authorFullName\":\"AuthorFN3\","
                        + "  \"title\":\"Hello\","
                        + "  \"content\":\"Contentdafadfadfadf ...\","
                        + "  \"category\":\"categoryadfadfadf\""
                        + "    },"
                        + "    {"
                        + "    \"authorFullName\":\"AutdafadhorFN4\","
                        + "   \"title\":\"Helloadfadfadf\","
                        + "    \"content\":\"Contentadfadf ...\","
                        + "    \"category\":\"categorydafad\""
                        + "   }"
                        + "  ]";

        try (CloseableHttpClient httpClient = HttpClients.createDefault();) {
            HttpPost req = new HttpPost(HOST + "/articles");
            req.setHeader("Content-Type", "application/json");
            StringEntity textBody = new StringEntity(body);
            req.setEntity(textBody);

            try (CloseableHttpResponse resp = httpClient.execute(req)) {
                HttpEntity entity = resp.getEntity();
                if (entity != null) {
                    String result = EntityUtils.toString(entity);
                    int statusCode = resp.getStatusLine().getStatusCode();

                    Assert.assertEquals(7, result.length());
                    Assert.assertEquals(201, statusCode);
                }
            }

        } catch (Exception e) {
            logger.error(e);
            Assert.fail();
        }

    }


    @Test
    public void perform3PutRequestToDWUsingJSONTest() {

        String body =
                "{"
                        + "\"id\": 1,"
                        + "\"authorFullName\":\"UPDATEDZZ\","
                        + "\"title\":\"ee\","
                        + "\"content\":\"rr ...\","
                        + "\"category\":\"tt\""
                        + "}";

        try (CloseableHttpClient httpClient = HttpClients.createDefault();) {
            HttpPut req = new HttpPut(HOST + "/articles");
            req.setHeader("Content-Type", "application/json");
            StringEntity textBody = new StringEntity(body);
            req.setEntity(textBody);

            try (CloseableHttpResponse resp = httpClient.execute(req)) {
                HttpEntity entity = resp.getEntity();
                if (entity != null) {
                    String result = EntityUtils.toString(entity);
                    int statusCode = resp.getStatusLine().getStatusCode();

                    Assert.assertEquals("SUCCESS", result);
                    Assert.assertEquals(201, statusCode);
                }
            }

        } catch (Exception e) {
            logger.error(e);
            Assert.fail();
        }

    }


    @Test
    public void perform4PutRequestToDWUsingXMLTest() {

        String body =
                "<item>"
                        + "<id>1</id>"
                        + "<authorFullName>QQQQQQ</authorFullName>"
                        + "<title>w: </title>"
                        + "<content>w:  ...</content>"
                        + "<category>w</category>"
                        + "</item>";

        try (CloseableHttpClient httpClient = HttpClients.createDefault();) {
            HttpPut req = new HttpPut(HOST + "/articles");
            req.setHeader("Content-Type", "application/xml");
            StringEntity textBody = new StringEntity(body);
            req.setEntity(textBody);

            try (CloseableHttpResponse resp = httpClient.execute(req)) {
                HttpEntity entity = resp.getEntity();
                if (entity != null) {
                    String result = EntityUtils.toString(entity);
                    int statusCode = resp.getStatusLine().getStatusCode();

                    Assert.assertEquals("SUCCESS", result);
                    Assert.assertEquals(201, statusCode);
                }
            }

        } catch (Exception e) {
            logger.error(e);
            Assert.fail();
        }

    }

    @Test
    public void perform5GetRequestToDWUsingJSONTest() {
        ArticleModel[] articles = new ArticleModel[] {};


        try (CloseableHttpClient httpClient = HttpClients.createDefault();) {

            HttpGet req = new HttpGet(HOST + "/articles?offset=0&limit=9999");
            req.setHeader("Content-Type", "application/json");

            try (CloseableHttpResponse resp = httpClient.execute(req)) {

                HttpEntity entity = resp.getEntity();
                if (entity != null) {
                    String result = EntityUtils.toString(entity);
                    ObjectMapper objectMapper = new ObjectMapper();
                    articles = objectMapper.readValue(result, ArticleModel[].class);


                    Assert.assertTrue(articles.length > 0);
                } else {
                    Assert.fail();
                }
            }

        } catch (Exception e) {
            Assert.fail();
        }
    }


    @Test
    public void perform6GetRequestToDWUsingXMLTest() {
        ArticleModel[] articles = new ArticleModel[] {};


        try (CloseableHttpClient httpClient = HttpClients.createDefault();) {

            HttpGet req = new HttpGet(HOST + "/articles?offset=0&limit=9999");
            req.setHeader("Content-Type", "application/xml");

            try (CloseableHttpResponse resp = httpClient.execute(req)) {

                HttpEntity entity = resp.getEntity();
                if (entity != null) {
                    String result = EntityUtils.toString(entity);
                    ObjectMapper objectMapper = new XmlMapper();
                    articles = objectMapper.readValue(result, ArticleModel[].class);


                    Assert.assertTrue(articles.length > 0);
                } else {
                    Assert.fail();
                }
            }

        } catch (Exception e) {
            Assert.fail();
        }
    }

}
