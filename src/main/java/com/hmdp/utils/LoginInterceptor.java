package com.hmdp.utils;

import com.hmdp.entity.User;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class LoginInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession();
        //从 session 中取出之前用户请求验证码时候的 session 保存的 user（如果有的话）
        Object user = session.getAttribute("user");
        if(user == null){
            //不存在，拦截，返回401状态码
            response.setStatus(401);
            return false;
        }
        //存在，保存用户信息到Threadlocal
        UserHolder.saveUser((User)user);
        //放行
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        HandlerInterceptor.super.afterCompletion(request, response, handler, ex);
    }
}
