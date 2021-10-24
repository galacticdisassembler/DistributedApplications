package com.ivan.data_warehouse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

/**
 * Hello world!
 *
 */

public class Main {

    private static final Logger logger = LogManager.getLogger(Main.class);

    public static void main(String[] args) {
        JettyHttpServer server = new JettyHttpServer();
        Map<String, String> argsVariablesWithValues = new HashMap<>();
        StringJoiner stringJoiner = new StringJoiner(",");

        for (String s : args) {
            stringJoiner.add(s);

            String[] argsVarAndVal = s.split("=");
            argsVariablesWithValues.put(argsVarAndVal[0], argsVarAndVal[1]);
        }
        logger.info("Args: {}", stringJoiner.toString());

        int port = Integer.parseInt(argsVariablesWithValues.getOrDefault("port", "8080"));
        try {
            server.start(port);
        } catch (Exception e) {
            logger.error(e);
        }
    }
}
