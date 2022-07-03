package cn.net.insurance.order.common.utils;

import cn.net.insurance.core.base.model.ExtraCodeEnum;
import cn.net.insurance.core.base.model.RespResult;
import cn.net.insurance.core.encipher.soft.SecretKeyFactory;
import cn.net.insurance.core.encipher.utils.SecretHttp;
import com.alibaba.nacos.common.utils.JacksonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

/**
 *服务调用工具类
 */
@Slf4j
@Component
@Order(-1)
public class ServiceExchangeUtils {
    //理赔公钥
    static final String serverPk = "MFkwEwYHKoZIzj0CAQYIKoEcz1UBgi0DQgAE4jVrfuhnhcuw59guQVJiFOfIGg3nq8/UTyTfLLdj4OLS2IAMDmemJ/ErLs/bWoW/eRzAt+dqYWDNqWMPTFG3dg==";
    //保单私钥
    static final String orderPrivateKey = "MIGTAgEAMBMGByqGSM49AgEGCCqBHM9VAYItBHkwdwIBAQQgItGNW5y/nlqOmngRFGnB+iWinFFzEloxDMolpjcFil2gCgYIKoEcz1UBgi2hRANCAAS5B1QSC041x8+HBjzP9vVJVaOyiqf5ZQWu/edg0xHQOQjTw58xI/krV6KLyYZPihsfN2IO5ECorDoDTJKa76NV";


    public static <T> RespResult<T> httpPostJson2ServiceOnMap(String url, Map<String, Object> params, Class<T> t) {
        try {
            String domainUrl = "http://127.0.0.1:9000/server" + url;
            if (params == null) {
                params = new HashMap<>();
            }
            params.put("subSystemNo", "13");
            log.info("请求服务系统,url:{},params:{}", domainUrl, JacksonUtils.toJson(params));

            PublicKey publicKey = SecretKeyFactory.getPublicKey(serverPk);
            PrivateKey privateKey = SecretKeyFactory.getPrivateKey(orderPrivateKey);
            RespResult result = SecretHttp.doPost(domainUrl, "13", params, 0, publicKey,privateKey);
            log.info("调用服务系统响应报文：{}",JacksonUtils.toJson(result));
            if (!result.result()) {
                log.warn("请求服务支撑系统响应结果为失败,url:{},resultcode:{}|{}", domainUrl, result.getErrorCode(), result.getErrorMsg());
                return RespResult.fail(result.getErrorCode(), result.getErrorMsg());
            } else {
                if (result.getData() != null && t != Void.class) {
                    if (!(result.getData() instanceof String)) {
                        result.setData(JacksonUtils.toObj(JacksonUtils.toJson(result.getData()), t));
                    }
                    if ((result.getData() instanceof String) && t != String.class) {
                        result.setData(JacksonUtils.toObj(result.getData().toString(), t));
                    }
                    return result;
                }
                return result;
            }
        } catch (Exception e) {
            log.error("请求服务支撑系统失败", e);
            return RespResult.fail(ExtraCodeEnum.SERVER_ERROR);
        }
    }


}
