package com.ivan.data_warehouse.servlets;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.ivan.data_warehouse.InternalSyncMechanism;
import javax.servlet.AsyncContext;
import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class CustomAsyncWriteListener implements WriteListener {

    private final String contentType;
    private final ServletContext servletContext;
    private final AsyncContext asyncContext;
    private final Object responseObject;
    private final InternalSyncMechanism internalSyncMechanism;
    private final HttpServletResponse response;

    public CustomAsyncWriteListener(String contentType, ServletContext servletContext,
            AsyncContext asyncContext, Object responseObject,
            InternalSyncMechanism internalSyncMechanism, HttpServletResponse response) {
        this.contentType = contentType;
        this.servletContext = servletContext;
        this.asyncContext = asyncContext;
        this.responseObject = responseObject;
        this.internalSyncMechanism = internalSyncMechanism;
        this.response = response;
    }

    @Override
    public void onWritePossible() throws IOException {
        String respString = "";

        if (contentType.equals("application/json")) {
            ObjectMapper objectMapper = new ObjectMapper();
            respString = objectMapper.writeValueAsString(responseObject);
        }

        if (contentType.equals("application/xml")) {
            XmlMapper xmlMapper = new XmlMapper();
            respString = xmlMapper.writeValueAsString(responseObject);
        }

        ByteBuffer content = ByteBuffer.wrap(respString.getBytes(StandardCharsets.UTF_8));
        ServletOutputStream outputStream = response.getOutputStream();

        while (outputStream.isReady()) {
            if (!content.hasRemaining()) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
                response.setStatus(200);
                asyncContext.complete();
                internalSyncMechanism
                        .getUsersCountIsServedNow()
                        .decrementAndGet();
                return;
            }
            outputStream.write(content.get());
        }

    }

    @Override
    public void onError(Throwable t) {
        servletContext.log("Async Error", t);
        response.setStatus(400);

        asyncContext.complete();
        internalSyncMechanism.getUsersCountIsServedNow().decrementAndGet();
    }
}
