package com.ivan.data_warehouse.servlets;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.ivan.data_warehouse.InternalSyncMechanism;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.http.HttpStatus;
import javax.servlet.AsyncContext;
import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class CustomAsyncWriteListener implements WriteListener {

    private static final Logger logger = LogManager.getLogger(CustomAsyncWriteListener.class);

    private final String contentType;
    private final ServletContext servletContext;
    private final AsyncContext asyncContext;
    private final Object responseObject;
    private final InternalSyncMechanism internalSyncMechanism;
    private final HttpServletResponse response;
    private final int responseStatus;

    private int devOnlyResponseDelayMilliseconds;

    private Runnable afterWriteCallback;
    private Runnable beforeWriteCallback;

    public CustomAsyncWriteListener(String contentType, ServletContext servletContext,
            AsyncContext asyncContext, Object responseObject, int responseStatus,
            InternalSyncMechanism internalSyncMechanism, HttpServletResponse response) {
        this.contentType = contentType;
        this.servletContext = servletContext;
        this.asyncContext = asyncContext;
        this.responseObject = responseObject;
        this.internalSyncMechanism = internalSyncMechanism;
        this.response = response;
        this.responseStatus = responseStatus;

        /**
         * Prevent NPE
         */
        this.afterWriteCallback = () -> {
        };
        this.beforeWriteCallback = () -> {
        };
    }

    public void setDevOnlyResponseDelayMilliseconds(int devOnlyResponseDelayMilliseconds) {
        this.devOnlyResponseDelayMilliseconds = devOnlyResponseDelayMilliseconds;
    }

    public void setAfterWriteCallback(Runnable afterWriteCallback) {
        this.afterWriteCallback = afterWriteCallback;
    }

    public void setBeforeWriteCallback(Runnable beforeWriteCallback) {
        this.beforeWriteCallback = beforeWriteCallback;
    }

    private void delay(int ms) {
        if (ms == 0) {
            return;
        }

        try {
            Thread.sleep(ms);
        } catch (InterruptedException ex) {
            logger.error(ex);
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void onWritePossible() throws IOException {

        try {
            String respString = "";

            if (contentType.equals("application/json")) {
                ObjectMapper objectMapper = new ObjectMapper();
                respString = objectMapper.writeValueAsString(responseObject);
            }

            if (contentType.equals("application/xml")) {
                XmlMapper xmlMapper = new XmlMapper();
                respString = xmlMapper.writeValueAsString(responseObject);
            }

            if (contentType.equals("text/plain")) {
                respString = responseObject.toString();
            }

            ServletOutputStream outputStream = response.getOutputStream();
            beforeWriteCallback.run();
            writeStringToOutputStream(outputStream, respString);
            afterWriteCallback.run();

            internalSyncMechanism.getUsersCountIsServedNow().decrementAndGet();

        } catch (Exception e) {
            response.setStatus(400);
            logger.error(e);
        }


    }

    @Override
    public void onError(Throwable t) {
        servletContext.log("Async Error", t);
        response.setStatus(HttpStatus.BAD_REQUEST_400);
        asyncContext.complete();
        internalSyncMechanism.getUsersCountIsServedNow().decrementAndGet();
    }

    private void writeStringToOutputStream(ServletOutputStream outputStream, String respString)
            throws IOException {
        ByteBuffer content = ByteBuffer.wrap(respString.getBytes(StandardCharsets.UTF_8));

        while (outputStream.isReady()) {
            if (!content.hasRemaining()) {

                delay(devOnlyResponseDelayMilliseconds);

                response.setStatus(responseStatus);
                asyncContext.complete();

                return;
            }

            outputStream.write(content.get());
        }

        content.clear();
    }
}
