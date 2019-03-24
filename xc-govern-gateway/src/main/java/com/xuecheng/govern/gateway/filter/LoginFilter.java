package com.xuecheng.govern.gateway.filter;

import com.alibaba.fastjson.JSON;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import com.xuecheng.framework.model.response.CommonCode;
import com.xuecheng.framework.model.response.ResponseResult;
import com.xuecheng.govern.gateway.service.AuthService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class LoginFilter extends ZuulFilter {

    @Autowired
    private AuthService authService;

    @Override
    public String filterType() {
        return FilterConstants.PRE_TYPE;
    }

    @Override
    public int filterOrder() {
        return FilterConstants.PRE_DECORATION_FILTER_ORDER - 1;
    }

    @Override
    public boolean shouldFilter() {
        return true;
    }

    @Override
    public Object run() throws ZuulException {
        // 获取上下文
        RequestContext requestContext = RequestContext.getCurrentContext();
        // 获取request
        HttpServletRequest request = requestContext.getRequest();
        //查询身份令牌
        String access_token = authService.getTokenFromCookie(request);
        if (StringUtils.isBlank(access_token)) {
            accessDenied(requestContext);
            return null;
        }
        //从redis中校验身份令牌是否过期
        boolean tokenFromRedis = authService.getTokenFromRedis(access_token);
        if (!tokenFromRedis) {
            accessDenied(requestContext);
            return null;
        }
        //查询jwt令牌
        String jwt = authService.getJwtFromHeader(request);
        if (StringUtils.isBlank(jwt)) {
            accessDenied(requestContext);
            return null;
        }
        return null;
    }

    //拒绝访问
    private void accessDenied(RequestContext requestContext) {
        //拒绝访问
        requestContext.setSendZuulResponse(false);
        //设置响应内容
        ResponseResult responseResult = new ResponseResult(CommonCode.UNAUTHENTICATED);
        String responseResultString = JSON.toJSONString(responseResult);
        requestContext.setResponseBody(responseResultString);
        //设置状态码
        requestContext.setResponseStatusCode(200);

        HttpServletResponse response = requestContext.getResponse();
        response.setContentType("application/json;charset=utf-8");
    }
}
