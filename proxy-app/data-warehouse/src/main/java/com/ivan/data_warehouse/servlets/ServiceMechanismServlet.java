package com.ivan.data_warehouse.servlets;

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

public class ServiceMechanismServlet extends HttpServlet {

    private InternalSyncMechanism internalSyncMechanism = InternalSyncMechanism.getInstance();

    private String getCurrentUsers() {
        return internalSyncMechanism.getUsersCountIsServedNow().toString();
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        internalSyncMechanism.getUsersCountIsServedNow().incrementAndGet();
        ByteBuffer content = ByteBuffer.wrap(getCurrentUsers().getBytes(StandardCharsets.UTF_8));

        AsyncContext async = request.startAsync();
        ServletOutputStream out = response.getOutputStream();

        out.setWriteListener(new WriteListener() {
            @Override
            public void onWritePossible() throws IOException {
                while (out.isReady()) {
                    if (!content.hasRemaining()) {
                        response.setStatus(200);
                        async.complete();
                        internalSyncMechanism.getUsersCountIsServedNow().decrementAndGet();
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
