package com.xuecheng.auth.service;

import com.alibaba.fastjson.JSON;
import com.xuecheng.framework.client.XcServiceList;
import com.xuecheng.framework.domain.ucenter.ext.AuthToken;
import com.xuecheng.framework.domain.ucenter.response.AuthCode;
import com.xuecheng.framework.exception.ExceptionCast;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Base64Utils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@Transactional
public class AuthService {

    @Autowired
    private LoadBalancerClient loadBalancerClient;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Value("${auth.tokenValiditySeconds}")
    private long tokenValiditySeconds;

    // redis中key的前缀
    private static final String KEY_PREFIX = "user_token:";

    //认证方法
    public AuthToken login(String username, String password, String clientId, String clientSecret) {
        //申请令牌
        AuthToken authToken = applyToken(username, password, clientId, clientSecret);
        //将 token存储到redis
        String access_token = authToken.getAccess_token();
        String content = JSON.toJSONString(authToken);
        boolean saveTokenResult = saveToken(access_token, content, tokenValiditySeconds);
        if (!saveTokenResult) {
            ExceptionCast.cast(AuthCode.AUTH_LOGIN_TOKEN_SAVEFAIL);
        }
        return authToken;
    }

    //存储令牌到redis
    private boolean saveToken(String access_token, String content, long tokenValiditySeconds) {
        //令牌名称
        String key = KEY_PREFIX + access_token;
        //保存到令牌到redis
        redisTemplate.boundValueOps(key).set(content, tokenValiditySeconds, TimeUnit.SECONDS);
        return redisTemplate.hasKey(key);
    }

    //认证方法
    private AuthToken applyToken(String username, String password, String clientId, String clientSecret) {
        //选中认证服务的地址
        ServiceInstance serviceInstance = loadBalancerClient.choose(XcServiceList.XC_SERVICE_UCENTER_AUTH);
        if (serviceInstance == null) {
            log.error("choose an auth instance fail");
            ExceptionCast.cast(AuthCode.AUTH_LOGIN_ERROR);
        }
        //获取令牌的url
        String path = serviceInstance.getUri().toString() + "/auth/oauth/token";
        //定义body
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        //授权方式
        formData.add("grant_type", "password");
        //账号
        formData.add("username", username);
        //密码
        formData.add("password", password);
        //定义头
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        httpHeaders.add("Authorization", httpbasic(clientId, clientSecret));
        //指定 restTemplate当遇到400或401响应时候也不要抛出异常，也要正常返回值
        restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            public void handleError(URI url, HttpMethod method, ClientHttpResponse response) throws IOException {
                //当响应的值为400或401时候也要正常响应，不要抛出异常
                if (response.getRawStatusCode() != 400 && response.getRawStatusCode() != 401) {
                    super.handleError(response);
                }
            }
        });
        Map body = null;
        try {
            //http请求spring security的申请令牌接口
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(formData, httpHeaders);
            ResponseEntity<Map> mapResponseEntity = restTemplate.postForEntity(path, request, Map.class);
            body = mapResponseEntity.getBody();
        } catch (RestClientException e) {
            log.error("request oauth_token_password error: {}", e.getMessage());
            ExceptionCast.cast(AuthCode.AUTH_LOGIN_ERROR);
        }
        //jti是jwt令牌的唯一标识作为用户身份令牌
        if (body == null ||
                body.get("access_token") == null ||
                body.get("refresh_token") == null ||
                body.get("jti") == null
        ) {
            //获取spring security返回的错误信息
            String error_description = (String) body.get("error_description");
            if (StringUtils.isBlank(error_description)) {
                ExceptionCast.cast(AuthCode.AUTH_LOGIN_ERROR);
            }
            if (error_description.equals("坏的凭证")) {
                ExceptionCast.cast(AuthCode.AUTH_CREDENTIAL_ERROR);
            }
            if (error_description.indexOf("UserDetailsService returned null") >= 0) {
                ExceptionCast.cast(AuthCode.AUTH_ACCOUNT_NOTEXISTS);
            }
        }
        AuthToken authToken = new AuthToken();
        //访问令牌(jwt)
        String jwt_token = (String) body.get("access_token");
        //刷新令牌(jwt)
        String refresh_token = (String) body.get("refresh_token");
        //jti，作为用户的身份标识
        String access_token = (String) body.get("jti");
        authToken.setAccess_token(access_token);
        authToken.setRefresh_token(refresh_token);
        authToken.setJwt_token(jwt_token);
        return authToken;
    }

    //获取httpbasic认证串
    private String httpbasic(String clientId, String clientSecret) {
        //将客户端id和客户端密码拼接，按“客户端id:客户端密码”
        String string = clientId + ":" + clientSecret;
        //进行base64编码
        byte[] encode = Base64Utils.encode(string.getBytes());
        return "Basic " + new String(encode);
    }

    //从redis查询令牌
    public AuthToken getUserToken(String access_token) {
        //令牌名称
        String key = KEY_PREFIX + access_token;
        String userTokenString = redisTemplate.opsForValue().get(key);
        if (StringUtils.isBlank(userTokenString)) {
            return null;
        }
        AuthToken authToken = null;
        try {
            authToken = JSON.parseObject(userTokenString, AuthToken.class);
        } catch (Exception e) {
            log.error("getUserToken from redis and execute JSON.parseObject error {}", e.getMessage());
            return null;
        }
        return authToken;
    }

    //从redis中删除令牌
    public void delToken(String access_token) {
        //令牌名称
        String key = KEY_PREFIX + access_token;
        redisTemplate.delete(key);
    }
}
