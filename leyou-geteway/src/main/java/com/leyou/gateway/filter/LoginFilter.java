package com.leyou.gateway.filter;

import com.leyou.common.utils.CookieUtils;
import com.leyou.common.utils.JwtUtils;
import com.leyou.gateway.config.FilterProperties;
import com.leyou.gateway.config.JwtProperties;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Component
@EnableConfigurationProperties({JwtProperties.class, FilterProperties.class})
public class LoginFilter extends ZuulFilter {
    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private FilterProperties filterProperties;

    @Override
    public String filterType() {
        return "pre";
    }


    @Override
    public int filterOrder() {
        return 10;
    }

    @Override
    public boolean shouldFilter() {

        List<String> allowPaths = this.filterProperties.getAllowPaths();

        RequestContext context = RequestContext.getCurrentContext();

        HttpServletRequest request = context.getRequest();

        String url = request.getRequestURL().toString();

        for (String allowPath : allowPaths) {
            if(StringUtils.contains(url,allowPath)){
                return false;
            }
            
        }
        
        return true;
    }

    @Override
    public Object run() throws ZuulException {

        RequestContext context = RequestContext.getCurrentContext();

        HttpServletRequest request = context.getRequest();



        String token = CookieUtils.getCookieValue(request, this.jwtProperties.getCookieName());



        try {
            JwtUtils.getInfoFromToken(token,jwtProperties.getPublicKey());
        } catch (Exception e) {
            e.printStackTrace();
            context.setSendZuulResponse(false);
            context.setResponseStatusCode(HttpStatus.UNAUTHORIZED.value());
        }


        return null;
    }
}
