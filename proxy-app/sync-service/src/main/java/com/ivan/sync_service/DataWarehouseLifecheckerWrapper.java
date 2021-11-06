package com.ivan.sync_service;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class DataWarehouseLifecheckerWrapper {
    public static Set<String> HOSTS = Collections.synchronizedSet(new LinkedHashSet<>());
    public static DataWarehouseLifechecker INSTANCE = new DataWarehouseLifechecker(HOSTS);
    public static Boolean connectedNewDW = Boolean.TRUE;

}
