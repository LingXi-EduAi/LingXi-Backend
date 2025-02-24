package com.lxe.lx.util;
import java.security.MessageDigest;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class MD5 {
    public static String md5(String str) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(str.getBytes());
            byte b[] = md.digest();

            int i;

            StringBuffer buf = new StringBuffer("");
            for (int offset = 0; offset < b.length; offset++) {
                i = b[offset];
                if (i < 0)
                    i += 256;
                if (i < 16)
                    buf.append("0");
                buf.append(Integer.toHexString(i));
            }
            str = buf.toString();
        } catch (Exception e) {
            e.printStackTrace();

        }
        return str;
    }

    public static void main(String[] args) throws ParseException {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String date1 = "2022-09-08 14:30:21";
        String date2 = "2022-09-07 14:30:21";
        Calendar nowDate = Calendar.getInstance();
        Calendar oldDate = Calendar.getInstance();
        nowDate.setTime(df.parse(date1));//设置为当前系统时间
        oldDate.setTime(df.parse(date2));//设置为想要比较的日期
        Long timeNow = nowDate.getTimeInMillis();
        Long timeOld = oldDate.getTimeInMillis();
        Long millis = (timeNow - timeOld);//相差毫秒数

        Long days = millis / (1000 * 60 * 60 * 24);
        Long hours = (millis % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60);

        Long minutes = (millis % (1000 * 60)) / (1000 * 60);
        Long seconds = (millis % (1000)) / 1000;
        String day = "";
        String hour = "";
        String minute = "";
        String second = "";
        if (days > 0 && days < 10) {
            day = "0" + days + " ";
        } else if (days > 10) {
            day = "" + days + " ";
        }
        if (hours < 10) {
            hour = "0" + hours;
        } else {
            hour = "" + hour;
        }
        if (minutes < 10) {
            minute = "0" + minutes;
        } else {
            minute = "" + minutes;
        }
        if (seconds < 10) {
            second = "0" + seconds;
        } else {
            second = "" + seconds;
        }
        // System.out.println(day + hour + ":" + minute + ":" + second);
    }
}
