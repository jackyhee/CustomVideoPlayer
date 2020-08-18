package com.jackyhee.customvideoplayer.utils;

/**
 * @author hexj
 * @createDate 2020/8/18 14:38
 **/
public class Utils {

    /**
     * 秒转换为时分，格式为“hh:mm:ss”
     * @param totalSeconds
     * @return
     */
    public static String second2TimeStr(Integer totalSeconds) {
        if (totalSeconds == null || totalSeconds < 1) {
            return "00:00";
        }
        //将秒格式化成HH:mm:ss
        int hours = totalSeconds / 3600;
        int rem = totalSeconds % 3600;
        int minutes = rem / 60;
        int seconds = rem % 60;
        if (hours <= 0) {
            return String.format("%02d:%02d", minutes, seconds);
        }
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}
