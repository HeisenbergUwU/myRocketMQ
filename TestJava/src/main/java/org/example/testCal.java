package org.example;

import java.time.ZoneId;
import java.util.Calendar;
import java.util.TimeZone;

public class testCal {
    public static void main(String[] args) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.add(Calendar.MONTH,1);
        System.out.println(cal);

        System.out.println(Calendar.getInstance());
    }
}
