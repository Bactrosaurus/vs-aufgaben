package de.uulm.in.vs.grn.vnscp.server.util;

import java.text.SimpleDateFormat;
import java.util.Date;

public class EventUtil {

    private static long currentId = 0;

    public static String getCurrentDate() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }

    public static long getNewId() {
        return currentId++;
    }

}
