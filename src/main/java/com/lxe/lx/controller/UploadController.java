package com.lxe.lx.controller;

import com.lxe.lx.util.FileUpload;
import com.lxe.lx.annotation.Login;
import com.lxe.lx.util.ResultConstant;
import com.lxe.lx.util.Tools;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;


import java.math.BigDecimal;
@RestController
@RequestMapping("/upload")
public class UploadController {
    Logger logger = LogManager.getLogger(UploadController.class);

    /**
     * 用于图片的上传，返回的是url
     *
     * @param file
     * @param folder
     * @param fileName
     * @return
     */
    @Login
    @RequestMapping(value = "/fileUpload", method = RequestMethod.POST)
    public ResultConstant fileUpload(@RequestParam(value = "file", required = false) MultipartFile file, String folder, String fileName) {
        try {
            if (checkFileSize(file)) {
                return ResultConstant.error("文件大小不能超过50M");
            }
            /**
             * 不实现新建文件夹，只在指定文件夹下上传文件
             */
//            String filePath = "D:\\uploadFilesTest\\file\\";
            String filePath = "\\home\\server\\eaiap\\uploadFilesTest\\file\\";
//            String filePath = "D:\\uploadFilesTest\\file\\" + folder;
            String newFileName = FileUpload.fileUp(file, Tools.getConfigValue("FILE_PATH_UPLOAD"), fileName);
            String fileUrl;
            if (StringUtils.isBlank(folder)) {
                fileUrl = Tools.getConfigValue("SERVER_PATH") + "uploadFilesTest/file/" + newFileName;
            } else {
                fileUrl = Tools.getConfigValue("SERVER_PATH") + "uploadFilesTest/file/" + folder + "/" + newFileName;
            }
            return ResultConstant.success(fileUrl);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("fileUpload->error"+e.getMessage());

            return ResultConstant.error("文件不存在");
        }

    }

    public static boolean checkFileSize(MultipartFile file) {
        String size = (Float.parseFloat(String.valueOf(file.getSize())) / 1024 / 1024) + "";
        BigDecimal b = new BigDecimal(size).divide(BigDecimal.ONE, 2, BigDecimal.ROUND_HALF_UP);
        if (b.compareTo(new BigDecimal("50")) > 0) {
            return true;
        } else {
            return false;
        }
    }
}

