package cn.net.insurance.order.gateway.config;

import lombok.Data;
import org.springframework.util.MultiValueMap;


@Data
public class GatewayContext {

    public static final String CACHE_GATEWAY_CONTEXT = "cacheWebGatewayContext";

    /**
     * cache json body
     */
    private String cacheBody;
    /**
     * cache formdata
     */
    private MultiValueMap<String, String> formData;
    /**
     * cache reqeust path
     */
    private String path;
}