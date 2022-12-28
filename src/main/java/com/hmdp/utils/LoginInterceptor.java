package com.hmdp.utils;

import cn.hutool.core.bean.BeanUtil;
import com.hmdp.dto.UserDTO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class LoginInterceptor implements HandlerInterceptor {

    public LoginInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    private StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //1.获取 token
        String token = request.getHeader("authorization");

        if (token.isBlank()) {
            //不存在，拦截，返回401状态码
            response.setStatus(401);
            return false;
        }
        //2 基于 token 从 redis 获取
        Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(RedisConstants.LOGIN_USER_KEY + token);
        if (userMap.isEmpty()) {
            //不存在，拦截，返回401状态码
            response.setStatus(401);
            return false;
        }
        //3. 转换回 userDTO
        UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false);

        //4， 存在，保存用户信息到当前线程的 threadlocals 变量里面（threadlocals 是一个存放 threadlocal 和对应 value 的 Map。这里 value 是 user）
        UserHolder.saveUser(userDTO);
        //5, 刷新 token 有效期
        stringRedisTemplate.expire(RedisConstants.LOGIN_USER_KEY + token, RedisConstants.LOGIN_USER_TTL, TimeUnit.MINUTES);
        //6. 放行
        return true;


    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserHolder.removeUser(); //当前是同一个线程，所以不需要指定 remove 的 user
        //移除当前 Localhost，防止内存泄露
    }
}
