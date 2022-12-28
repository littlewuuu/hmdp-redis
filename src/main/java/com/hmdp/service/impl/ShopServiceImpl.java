package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.beans.Transient;
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
        //1. 从 redis 里面查询
        String shopCacheJson = stringRedisTemplate.opsForValue().get(RedisConstants.CACHE_SHOP_KEY + id);
        if(StrUtil.isNotBlank(shopCacheJson)){ //2.有就转成 shop 对象直接返回
            Shop shop = JSONUtil.toBean(shopCacheJson, Shop.class);
            return Result.ok(shop);
        }
        //3. 没有就从数据库里面查询
        Shop shop = getById(id);
        //4. 没有返回错误
        if(shop == null){
            return Result.fail("no such shop");
        }
        //5. 数据库里面有，需要添加到 redis 缓存里面，添加超时时间，兜底方案（缓存一致性）
        stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_KEY + id,JSONUtil.toJsonStr(shop),RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
        //6. 返回店铺信息
        return  Result.ok(shop);
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
}
