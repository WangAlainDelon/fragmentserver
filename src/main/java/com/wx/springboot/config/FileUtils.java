package com.wx.springboot.config;


import org.apache.commons.lang3.StringUtils;

/**
 * <p>
 * 文件服务工具类
 * </p>
 *
 * @author qingsheng.chen 2018/7/12 Thursday 21:19
 */
public class FileUtils {

    private FileUtils() {
    }

    /**
     * 从文件下载链接中解析文件名
     *
     * @param fileUrl 文件下载链接
     * @return 文件名
     */
    public static String getFileName(String fileUrl) {
        if (StringUtils.isBlank(fileUrl)) {
            return null;
        }
        // 取分割的最后一段
        int index = fileUrl.lastIndexOf("/");
        if (index < 0) {
            return fileUrl;
        }
        String uuidFilename = fileUrl.substring(index + 1);
        if (!uuidFilename.contains("@") || uuidFilename.length() < 33) {
            return uuidFilename;
        }
        // 判断第33个字符是否是@符号
        if (uuidFilename.charAt(32) == '@') {
            return uuidFilename.substring(33);
        } else {
            return uuidFilename;
        }
    }
}
