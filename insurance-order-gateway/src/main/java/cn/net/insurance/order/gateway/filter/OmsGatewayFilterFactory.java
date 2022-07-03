package cn.net.insurance.order.gateway.filter;

import cn.net.insurance.core.base.model.ExtraCodeEnum;
import cn.net.insurance.order.common.constant.RedisKeys;
import cn.net.insurance.order.common.entity.AccountCacheDto;
import cn.net.insurance.order.common.enums.SystemTypeEnum;
import cn.net.insurance.order.gateway.config.AdminIgnoreWhiteProperties;
import cn.net.insurance.order.gateway.utils.GatewayResponseUtils;
import cn.net.insurance.order.gateway.utils.InsuranceStringUtils;
import com.alibaba.nacos.common.utils.JacksonUtils;
import com.alibaba.nacos.common.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 验证token以及授权
 */
@Slf4j
@Component
@DependsOn(value = "webGlobalFilter")
public class OmsGatewayFilterFactory extends AbstractGatewayFilterFactory {
    private static final String AUTHORIZE_USER_ID = "userId";
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Value("${token.expire.seconds}")
    private long timeOut;
    @Autowired
    private AdminIgnoreWhiteProperties adminIgnoreWhiteProperties;

    @Override
    public GatewayFilter apply(Object config) {
        return ((exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            ServerHttpResponse response = exchange.getResponse();
            String path = request.getPath().pathWithinApplication().value();
            if (InsuranceStringUtils.matches(path, adminIgnoreWhiteProperties.getWhites())) {
                return chain.filter(exchange);
            }
            HttpHeaders headers = request.getHeaders();
            String userId = headers.getFirst(AUTHORIZE_USER_ID);
            if (StringUtils.isBlank(userId)) {
                log.warn("userId不存在:{}",userId);
                return GatewayResponseUtils.writeResponse(response, ExtraCodeEnum.INVALID_TOKEN);
            }
            String systemType = SystemTypeEnum.INSURANCE.getName();
            String redisUser = String.format(RedisKeys.OMS_LOGIN_USER_ID, systemType, userId);
            if (!redisTemplate.hasKey(redisUser)) {
                log.warn("当前登陆用户token不一致,userId:{}",userId);
                return GatewayResponseUtils.writeResponse(response, ExtraCodeEnum.INVALID_TOKEN);
            }
            String token = null;
            String accountCacheDtoJson = redisTemplate.opsForValue().get(redisUser);
            if(StringUtils.isNotBlank(accountCacheDtoJson)){
                AccountCacheDto accountCacheDto = JacksonUtils.toObj(accountCacheDtoJson, AccountCacheDto.class);
                token = accountCacheDto.getToken();
            }
            if (StringUtils.isBlank(token)) {
                return GatewayResponseUtils.writeResponse(response, ExtraCodeEnum.ERROR_PARAMS.code, "TOKEN不存在");
            }
            String redisToken = String.format(RedisKeys.OMS_LOGIN_USER_TOKEN, systemType, token);
            //如果有接口请求,直接重新设置token时长
            redisTemplate.expire(redisUser, timeOut, TimeUnit.MINUTES);
            redisTemplate.expire(redisToken, timeOut, TimeUnit.MINUTES);
            return chain.filter(exchange);
        });
    }

}