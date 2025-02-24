package com.lxe.lx.util;

//import org.apache.poi.xssf.usermodel.XSSFCell;
//import org.apache.poi.xssf.usermodel.XSSFRow;
//import org.apache.poi.xssf.usermodel.XSSFSheet;
//import org.apache.poi.xssf.usermodel.XSSFWorkbook;
//import org.jaudiotagger.audio.AudioFileIO;
//import org.jaudiotagger.audio.mp3.MP3AudioHeader;
//import org.jaudiotagger.audio.mp3.MP3File;

import javax.servlet.http.HttpServletRequest;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.io.*;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.compile;

public class Tools {
    /**检验电话号码
     * @return
     */
    public static boolean checkMobileNumber(String mobileNumber) {
        boolean flag = false;
        try {
//	    Pattern regex = Pattern.compile("^(((13[0-9])|(15([0-3]|[5-9]))|(18[0,5-9]))\\d{8})|(0\\d{2}-\\d{8})|(0\\d{3}-\\d{7})$");
//		Pattern regex = compile("^(0|86|17951)?(13[0-9]|15[012356789]|16[6]|19[89]]|17[01345678]|18[0-9]|14[579])[0-9]{8}$");
//		Pattern regex = compile("^0?(13|14|15|18|17)[0-9]{9}$");
            Pattern regex = compile("^1[345789]\\d{9}$");//手机号验证和前端保持一致
            Matcher matcher = regex.matcher(mobileNumber);
            flag = matcher.matches();
        } catch (Exception e) {
            flag = false;
        }
        return flag;
    }
    /**得到当前时间
     * @return
     */
    public static String nowTimeStr() {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return df.format(new Date());
    }

    /**
     * 获取ip地址
     * @param request
     * @return
     * @throws Exception
     */
    public static String getIp(HttpServletRequest request) throws Exception {
        String ipAddress = request.getHeader("x-forwarded-for");
        if (ipAddress == null || ipAddress.length() == 0 || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.length() == 0 || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.length() == 0 || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
            if (ipAddress.equals("127.0.0.1") || ipAddress.equals("0:0:0:0:0:0:0:1")) {
                //根据网卡取本机配置的IP
                InetAddress inet = null;
                try {
                    inet = InetAddress.getLocalHost();
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
                ipAddress = inet.getHostAddress();
            }
        }
        //对于通过多个代理的情况，第一个IP为客户端真实IP,多个IP按照','分割
        if (ipAddress != null && ipAddress.length() > 15) { //"***.***.***.***".length() = 15
            if (ipAddress.indexOf(",") > 0) {
                ipAddress = ipAddress.substring(0, ipAddress.indexOf(","));
            }
        }
        return ipAddress;
    }
    /**得到32位的uuid
     * @return
     */
    public static String get32UUID(){
        String uuid = UUID.randomUUID().toString().replace("-", "");
        uuid.replace("/","");
        return uuid;
    }
    public static String getConfigValue(String key) throws IOException {
        String config_path = Const.CONFIG_PATH;
        //固定路径读取
        InputStream ins = new BufferedInputStream(new FileInputStream(config_path + "application.properties"));
        Properties properties1 = new Properties();
        properties1.load(ins);
        String value = properties1.getProperty(key);
        return value;
    }

    /**
     * 获取第二个数组
     * @param filePath
     * @return
     * @throws IOException
     */
    public static byte[] toByteArray2(String filePath) throws IOException {
        File f = new File(filePath);
        if (!f.exists()) {
            throw new FileNotFoundException(filePath);
        }
        FileChannel channel = null;
        FileInputStream fs = null;
        try {
            fs = new FileInputStream(f);
            channel = fs.getChannel();
            ByteBuffer byteBuffer = ByteBuffer.allocate((int) channel.size());
            while ((channel.read(byteBuffer)) > 0) {
                // do nothing
                // System.out.println("reading");
            }
            return byteBuffer.array();
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        } finally {
            try {
                channel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                fs.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }



//    public static List<String> readExcel(String filePath) throws Exception {
//        XSSFWorkbook xwb = new XSSFWorkbook(filePath);
//        List<String> list = new ArrayList<>();
//        //循环工作表sheet
//        for (int numSheet = 0; numSheet < xwb.getNumberOfSheets(); numSheet++) {
//            XSSFSheet xSheet = xwb.getSheetAt(numSheet);
//            if (xSheet == null) {
//                continue;
//            }
//            //循环行row
//            for (int numRow = 0; numRow <= xSheet.getLastRowNum(); numRow++) {
//                XSSFRow xRow = xSheet.getRow(numRow);
//                if (xRow == null) {
//                    continue;
//                }
//                //循环列cell
//                for (int numCell = 1; numCell <= xRow.getLastCellNum(); numCell++) {
//
//                    XSSFCell xCell = xRow.getCell(numCell);
//                    if (xCell == null) {
//                        continue;
//                    }
//                    //输出值
//
////                    System.out.println("excel表格中取出的数据" + getValue(xCell));
//                    String temp = getValue(xCell);
//                    list.add(temp);
//                }
//            }
//        }
//
//        return list;
//    }
//    private static String getValue(XSSFCell xCell) {
//        if (xCell.getCellType() == XSSFCell.CELL_TYPE_BOOLEAN) {
//            return String.valueOf(xCell.getBooleanCellValue());
//        } else if (xCell.getCellType() == XSSFCell.CELL_TYPE_NUMERIC) {
//            return String.valueOf(xCell.getNumericCellValue());
//        } else {
//            return String.valueOf(xCell.getStringCellValue());
//        }
//    }
//

    /**
     * 二进制流转音频文件
     * @param binaryData
     * @param outputFilePath
     * @throws IOException
     */
    public static boolean convertBinaryToAudio(byte[] binaryData, String outputFilePath) throws IOException {
        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(outputFilePath);
            outputStream.write(binaryData);
            return true;
        }catch (Exception e){
            return false;
        }finally {
            if (outputStream != null) {
                outputStream.close();
            }
        }
    }
    /**
     * 获取AMR格式音频长度
     * @param file
     * @return
     * @throws IOException
     */
    public static int getAmrDuration(File file) throws IOException {
        long duration = -1;
        int[] packedSize = { 12, 13, 15, 17, 19, 20, 26, 31, 5, 0, 0, 0, 0, 0,
                0, 0 };
        RandomAccessFile randomAccessFile = null;
        try {
            randomAccessFile = new RandomAccessFile(file, "rw");
            // 文件的长度
            long length = file.length();
            // 设置初始位置
            int pos = 6;
            // 初始帧数
            int frameCount = 0;
            int packedPos = -1;
            // 初始数据值
            byte[] datas = new byte[1];
            while (pos <= length) {
                randomAccessFile.seek(pos);
                if (randomAccessFile.read(datas, 0, 1) != 1) {
                    duration = length > 0 ? ((length - 6) / 650) : 0;
                    break;
                }
                packedPos = (datas[0] >> 3) & 0x0F;
                pos += packedSize[packedPos] + 1;
                frameCount++;
            }
            // 帧数*20
            duration += frameCount * 20;
        } catch (Exception e){
        }finally {
            if (randomAccessFile != null) {
                randomAccessFile.close();
            }
        }
        return (int)((duration/1000)+1);
    }

//    /**
//     * 计算Mp3音频格式时长
//     * @param mp3File
//     * @return
//     */
//    public static int getMp3Duration(File mp3File) {
//        try {
//            MP3File f = (MP3File) AudioFileIO.read(mp3File);
//            MP3AudioHeader audioHeader = (MP3AudioHeader) f.getAudioHeader();
//            return audioHeader.getTrackLength();
//        } catch (Exception e) {
//            return 0;
//        }
//    }

    public static int getWavLength(String wavFilePath) {
        //网络路径，返回的是秒
        if (wavFilePath == null || wavFilePath.length() == 0) {
            return 0;
        }
        Clip clip =null;
        try {
            File source = new File(wavFilePath);
            clip = AudioSystem.getClip();
            AudioInputStream ais = AudioSystem.getAudioInputStream(source);
            clip.open(ais);
            double temp = clip.getMicrosecondLength() / 1000000D;
            int returnTemp = (int)temp;
            return returnTemp;
        } catch (Exception e) {
//            log.info("获取语音时长失败，错误信息：{}", e);
            return 0;
        } finally {
            if(clip!=null){
                clip.close();
            }
        }
    }
    private static final String EMAIL_REGEX = "^[\\w.-]+@[a-zA-Z\\d.-]+\\.[a-zA-Z]{2,6}$";
    private static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX);

    public static boolean isValidEmail(String email) {
        if (email == null) {
            return false;
        }
        Matcher matcher = EMAIL_PATTERN.matcher(email);
        return matcher.matches();
    }
    public static String generateCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }

//    public static void main(String[] args) throws IOException {
////        String path="D:/uploadFilesTest/file/周杰伦 - 等你下课 (with 杨瑞代).mflac";
////        int duration = getMp3Duration(new File(path));
////        System.out.println(duration);
//        String path = "D:/uploadFilesTest/file/b4104a5bc01c4c57879745b5eb33a44c.wav";
//        String ext = path.substring(path.lastIndexOf(".") + 1).toLowerCase();
//        System.out.println(ext);
//    }
//

}
