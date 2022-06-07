package com.wx.springboot.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

/**
 * Token 相关常量
 *
 * @author bojiangzhou 2020/06/24
 */
public final class TokenConstants {

    private static final Logger logger = LoggerFactory.getLogger(TokenConstants.class);

    public static final String HEADER_JWT = "Jwt_Token";
    public static final String HEADER_BEARER = "Bearer";
    public static final String DEFAULT_HEADER_AUTH = "Authorization";
    public static final String HEADER_AUTH = getAuthHeaderName();
    public static final String ACCESS_TOKEN = "access_token";
    public static final String JWT_TOKEN = "jwt_token";

    private static CoreProperties coreProperties;

    /**
     * @return 认证令牌请求头名称
     */
    public static String getAuthHeaderName() {
        return DEFAULT_HEADER_AUTH;
//        if (coreProperties == null) {
//            synchronized (BaseHeaders.class) {
//                ApplicationContext context = ApplicationContextHelper.getContext();
//                if (context != null) {
//                    coreProperties = context.getBean(CoreProperties.class);
//                }
//            }
//            if (coreProperties == null) {
//                logger.warn("Not found bean of CoreProperties, use default value [{}].", DEFAULT_HEADER_AUTH);
//                return DEFAULT_HEADER_AUTH;
//            }
//        }
//        return coreProperties.getResource().getAuthHeaderName();
    }

}
