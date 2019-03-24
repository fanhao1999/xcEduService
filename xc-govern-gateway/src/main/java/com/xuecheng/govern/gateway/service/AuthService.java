package com.xuecheng.govern.gateway.service;

import com.xuecheng.framework.utils.CookieUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@Service
@Transactional
public class AuthService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    // redis中key的前缀
    private static final String KEY_PREFIX = "user_token:";

    //查询身份令牌
    public String getTokenFromCookie(HttpServletRequest request) {
        Map<String, String> cookieMap = CookieUtil.readCookie(request, "uid");
        if (cookieMap == null || StringUtils.isBlank(cookieMap.get("uid"))) {
            return  null;
        }
        String access_token = cookieMap.get("uid");
        return access_token;
    }

    //从header中查询jwt令牌
    public String getJwtFromHeader(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (StringUtils.isBlank(authorization)) {
            //拒绝访问
            return null;
        }
        if (!authorization.startsWith("Bearer ")) {
            //拒绝访问
            return null;
        }
        String jwt = authorization.substring(7);
        return jwt;
    }

    //查询令牌的有效期
    public boolean getTokenFromRedis(String access_token) {
        //令牌名称
        String key = KEY_PREFIX + access_token;
        return redisTemplate.hasKey(key);
    }
}
