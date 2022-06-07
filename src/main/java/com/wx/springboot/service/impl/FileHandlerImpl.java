package com.wx.springboot.service.impl;

import com.wx.springboot.service.FileHandler;
import java.io.InputStream;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Created by wangxiang on 2022/6/8
 */
@Component
public class FileHandlerImpl  implements FileHandler {

    @Override
    public String process(Long tenantId, String filename, String filePath, InputStream inputStream, Map<String, String> params) {
        return "/file/real/1-1 导学必看.mp4";

    }
}
