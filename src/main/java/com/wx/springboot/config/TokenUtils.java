package com.wx.springboot.config;



import javax.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ServerWebExchange;

/**
 * @author qingsheng.chen@hand-china.com
 */
public class TokenUtils {
    public static final String HEADER_LABEL = "X-Eureka-Label";
    public static final String HEADER_JWT = TokenConstants.HEADER_JWT;
    public static final String HEADER_BEARER = TokenConstants.HEADER_BEARER;
    public static final String HEADER_AUTH = TokenConstants.HEADER_AUTH;
    public static final String ACCESS_TOKEN = TokenConstants.ACCESS_TOKEN;
    /**
     * @return 获取当前登陆客户端 token
     */
    public static String getToken() {
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        String accessToken = requestAttributes.getRequest().getHeader(HEADER_AUTH);
        if (!StringUtils.hasText(accessToken)) {
            accessToken = requestAttributes.getRequest().getParameter(ACCESS_TOKEN);
        }
        return trimToken(accessToken);
    }

    public static String getToken(HttpServletRequest request) {
        String accessToken = request.getHeader(HEADER_AUTH);
        if (!StringUtils.hasText(accessToken)) {
            accessToken = request.getParameter(ACCESS_TOKEN);
        }
        if (!StringUtils.hasText(accessToken)) {
            accessToken = (String) request.getAttribute(ACCESS_TOKEN);
        }
        return trimToken(accessToken);
    }

    public static String getToken(ServerWebExchange exchange) {
        HttpHeaders headers = exchange.getRequest().getHeaders();
        String accessToken = headers.getFirst(HEADER_AUTH);
        if (!StringUtils.hasText(accessToken)) {
            MultiValueMap<String, String> queryParams = exchange.getRequest().getQueryParams();
            accessToken = queryParams.getFirst(ACCESS_TOKEN);
        }
        if (!StringUtils.hasText(accessToken)) {
            accessToken = exchange.getAttribute(ACCESS_TOKEN);
        }
        return trimToken(accessToken);
    }

    /**
     * 获取token，并去掉token的标识前缀和空格
     *
     * @param accessToken token
     * @return 整理后的token
     */
    private static String trimToken(String accessToken) {
        return StringUtils.startsWithIgnoreCase(accessToken, HEADER_BEARER) ?
                accessToken.substring((HEADER_BEARER).length()).trim() : accessToken;
    }
}
