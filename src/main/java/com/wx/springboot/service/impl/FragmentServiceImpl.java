package com.wx.springboot.service.impl;


import com.wx.springboot.config.ApplicationContextHelper;
import com.wx.springboot.config.BaseConstants;
import com.wx.springboot.config.FragmentConfig;
import com.wx.springboot.config.Pair;
import com.wx.springboot.service.FileHandler;
import com.wx.springboot.service.FragmentService;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * description
 *
 * @author shuangfei.zhu@hand-china.com 2020/02/18 14:07
 */
@Service
public class FragmentServiceImpl implements FragmentService {

    private static final Logger logger = LoggerFactory.getLogger(FragmentServiceImpl.class);

    public static final String ROOT = System.getProperty("user.dir") + File.separator + "file";
    public static final String TEMP = "temp";
    public static final String REAL = "real";

    @Autowired
    private FragmentConfig fragmentConfig;

    private String getRootPath() {
        String rootPath = fragmentConfig.getRootPath();
        if (StringUtils.isEmpty(rootPath)) {
            return ROOT;
        }
        return rootPath;
    }

    @Override
    public Integer checkMd5(String chunk, String chunkSize, String guid) {
        // 分片上传路径
        String tempPath = getRootPath() + File.separator + TEMP;
        File checkFile = new File(tempPath + File.separator + guid + File.separator + chunk);
        // 如果当前分片存在，并且长度等于上传的大小
        if (checkFile.exists() && checkFile.length() == Integer.parseInt(chunkSize)) {
            return BaseConstants.Flag.YES;
        } else {
            return BaseConstants.Flag.NO;
        }
    }

    @Override
    public void upload(MultipartFile file, Integer chunk, String guid) {
        try (InputStream inputStream = file.getInputStream()) {
            if (chunk == null) {
                chunk = 0;
            }
            String filePath = getRootPath() + File.separator + TEMP + File.separator + guid + File.separator + chunk;
            FileUtils.copyInputStreamToFile(inputStream, new File(filePath));
        } catch (Exception e) {
            throw new RuntimeException(BaseConstants.ErrorCode.ERROR, e);
        }
    }

    @Override
    public Pair<String, String> combineBlock(String guid, String fileName) {
        // 分片文件临时目录
        String tempDir = getRootPath() + File.separator + TEMP + File.separator + guid;
        File tempPath = new File(tempDir);
        // 真实上传路径
        File realPath = new File(getRootPath() + File.separator + REAL);
        if (!realPath.exists()) {
            Assert.isTrue(realPath.mkdirs(), "Create file directory error.");
        }
        String filePath = getRootPath() + File.separator + REAL + File.separator + fileName;
        File realFile = new File(filePath);
        // 文件追加写入
        try (FileOutputStream os = new FileOutputStream(realFile, true)) {
            logger.info("file start to merge, filename : {}, MD5 : {}", fileName, guid);
            if (!tempPath.exists()) {
                throw new RuntimeException("read file error");
            }
            // 获取临时目录下的所有文件
            File[] tempFiles = tempPath.listFiles();
            Assert.notNull(tempFiles, BaseConstants.ErrorCode.ERROR);
            // 按名称排序
            Arrays.sort(tempFiles, Comparator.comparingInt(o -> Integer.parseInt(o.getName())));
            byte[] bytes = new byte[1024 * 1024];
            for (File file : tempFiles) {
                FileInputStream fis = new FileInputStream(file);
                int len;
                while ((len = fis.read(bytes)) != -1) {
                    os.write(bytes, 0, len);
                }
                fis.close();
            }
            logger.info("file merged successfully!  filename : {}, MD5 : {}", fileName, guid);
            return Pair.of(filePath, tempDir);
        } catch (IOException e) {
            throw new RuntimeException(BaseConstants.ErrorCode.ERROR, e);
        }
    }

    @Override
    public String combineUpload(String guid, Long tenantId, String filename, Map<String, String> params) {
        if (params == null) {
            params = new HashMap<>(1);
            params.put("key","value");
        }
        Pair<String, String> pair = combineBlock(guid, filename);
        String filePath = pair.getFirst();
        String tempDtr = pair.getSecond();
        try {
            Map<String, FileHandler> map = ApplicationContextHelper.getContext().getBeansOfType(FileHandler.class);
            String url = null;
            if (!map.isEmpty()) {
                for (FileHandler handler : map.values()) {
                    url = handler.process(tenantId, filename, filePath, new FileInputStream(filePath), params);
                }
            }
            return url;
        } catch (Exception ex) {
            throw new RuntimeException("fragment.error.combine", ex);
        } finally {
            // 删除分片
            deleteFile(tempDtr);
            // 删除文件
            deleteFile(filePath);
        }
    }

    /**
     * 删除文件
     *
     * @param path 文件路径
     */
    private void deleteFile(String path) {
        try {
            FileUtils.deleteDirectory(new File(path));
        } catch (Exception e) {
            logger.error("Delete file error! file path : {}", path);
        }
    }
}
