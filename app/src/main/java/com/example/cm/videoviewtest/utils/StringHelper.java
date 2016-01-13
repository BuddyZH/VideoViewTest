package com.example.cm.videoviewtest.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Created by cm on 2016/1/5.
 */
public class StringHelper {
    public static String stringForTime(int timeMs) {
        int totalSeconds = timeMs / 1000;

        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours = totalSeconds / 3600;

        return hours > 0 ? String.format("%02d:%02d:%02d", hours, minutes, seconds) : String.format("%02d:%02d", minutes, seconds);
    }
    public static String stringForCurrentTime(){
        return new SimpleDateFormat("HH:mm").format(Calendar.getInstance().getTime());
    }
}
