package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RedisData;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.beans.Transient;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAmount;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryById(Long id) {
        /*缓存穿透解决方案
        Shop shop = queryWithPassThrough(id);
         */

        //互斥锁解决缓存击穿
        Shop shop = queryWithMutex(id);
        if(shop == null){
            return Result.fail("no such shop");
        }

        return  Result.ok(shop);
    }

    /**
     * 互斥锁解决缓存击穿
     */
    public  Shop queryWithMutex(Long id){
        //1. 从 redis 里面查询
        String shopCacheJson = stringRedisTemplate.opsForValue().get(RedisConstants.CACHE_SHOP_KEY + id);
        if(StrUtil.isNotBlank(shopCacheJson)){ //2.有就转成 shop 对象直接返回
            Shop shop = JSONUtil.toBean(shopCacheJson, Shop.class);
            return shop;
        }
        //判断的命中是否是空字符串
        if(shopCacheJson !=null){
            //shopCacheJson =="" 说明是之前防止缓存穿透存入的"" 空值
            return null;
        }

        //3. 没有就从数据库里面查询
        //3.1 获取互斥锁
        String lockKey = RedisConstants.LOCK_SHOP_KEY + id;
        Shop shop = null;
        try {
            boolean isLock = tryLock(lockKey);
            //3.2 判断是否获得互斥锁
            if (!isLock) {
                //3.3 失败则休眠并重试
                Thread.sleep(50);
                return queryWithMutex(id);
            }
            //3.4 获取互斥锁成功则查询数据库并写入缓存
            shop = getById(id);
            //模拟缓存重建时延
            Thread.sleep(200);
            //4. 没有返回错误
            if(shop == null){
                //将id 对应的空字符串(注意不是 null)写入 redis，防止缓存穿透
                stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_KEY + id,"",RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
                return null;
            }
            //5. 数据库里面有，需要添加到 redis 缓存里面，添加超时时间，兜底方案（缓存一致性）
            stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_KEY + id,JSONUtil.toJsonStr(shop),RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);

        }catch (Exception e){
            throw new RuntimeException(e);
        }finally {
            //6. 释放互斥锁
            unlock(lockKey);
        }
        //7. 返回店铺信息
        return shop;
    }

    /**
     * 缓存穿透的解决方案
     * @param id
     * @return
     */
    public Shop queryWithPassThrough(Long id){
        //1. 从 redis 里面查询
        String shopCacheJson = stringRedisTemplate.opsForValue().get(RedisConstants.CACHE_SHOP_KEY + id);
        if(StrUtil.isNotBlank(shopCacheJson)){ //2.有就转成 shop 对象直接返回
            Shop shop = JSONUtil.toBean(shopCacheJson, Shop.class);
            return shop;
        }
        //判断的命中是否是空字符串
        if(shopCacheJson !=null){
            //shopCacheJson =="" 说明是之前防止缓存穿透存入的"" 空值
            return null;
        }
        //3. 没有就从数据库里面查询
        Shop shop = getById(id);
        //4. 没有返回错误
        if(shop == null){
            //将id 对应的空字符串(注意不是 null)写入 redis，防止缓存穿透
            stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_KEY + id,"",RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }
        //5. 数据库里面有，需要添加到 redis 缓存里面，添加超时时间，兜底方案（缓存一致性）
        stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_KEY + id,JSONUtil.toJsonStr(shop),RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
        //6. 返回店铺信息
        return shop;
    }

    public Shop queryWithLogicalExpire(Long id){
        //1. 从 redis 里面查询
        String shopCacheJson = stringRedisTemplate.opsForValue().get(RedisConstants.CACHE_SHOP_KEY + id);
        if(StrUtil.isBlank(shopCacheJson)){ //2.不存在直接返回
            return null;
        }
        //3. 没有就从数据库里面查询
        Shop shop = getById(id);

        //5. 数据库里面有，需要添加到 redis 缓存里面，添加超时时间，兜底方案（缓存一致性）
        stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_KEY + id,JSONUtil.toJsonStr(shop),RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
        //6. 返回店铺信息
        return shop;
    }

    //尝试获取互斥锁
    private boolean tryLock(String key) { //返回值是boolean 而 flag 是 Boolean
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);//添加 ttl 防止 unlock 执行不了的时候一直被锁
        return BooleanUtil.isTrue(flag);
    }

    //删除互斥锁
    private void unlock(String key) {
        stringRedisTemplate.delete(key);
    }

    @Override
    @Transactional //注意要开启事务
    public Result update(Shop shop) {
        if(shop.getId() == null){
            return Result.fail("shop id can't be null");
        }
        //1、更新数据库
        updateById(shop);
        //2、删除缓存
        stringRedisTemplate.delete(RedisConstants.CACHE_SHOP_KEY + shop.getId());
        return Result.ok();
    }

    /**
     * 逻辑过期时间
     * @param id
     * @param expireSeconds
     */
    public void saveShop2Reedis(Long id, Long expireSeconds){
        //查询出shop
        Shop shop = getById(id);
        //封装成RedisData
        RedisData redisData = new RedisData();
        redisData.setData(shop);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));
        //存入 redis
        stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_KEY + id,JSONUtil.toJsonStr(redisData));

    }
}
