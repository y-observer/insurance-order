package cn.net.insurance.order.gateway.filter;

import cn.hutool.core.map.MapUtil;
import cn.net.insurance.core.base.model.GwInsuranceException;
import cn.net.insurance.core.base.model.ExtraCodeEnum;
import cn.net.insurance.core.base.model.RespResult;
import cn.net.insurance.core.common.utils.SoftSM3Util;
import cn.net.insurance.order.common.constant.RedisKeys;
import cn.net.insurance.order.common.enums.SystemTypeEnum;
import cn.net.insurance.order.gateway.config.AdminIgnoreWhiteProperties;
import cn.net.insurance.order.gateway.config.GatewayContext;
import cn.net.insurance.order.gateway.utils.GatewayResponseUtils;
import cn.net.insurance.order.gateway.utils.InsuranceStringUtils;
import com.alibaba.nacos.common.utils.JacksonUtils;
import io.netty.buffer.ByteBufAllocator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.factory.rewrite.CachedBodyOutputMessage;
import org.springframework.cloud.gateway.support.BodyInserterContext;
import org.springframework.core.Ordered;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author shawoo
 */
@Slf4j
@RefreshScope
@Component
public class WebGlobalFilter implements GlobalFilter, Ordered {
    private static final String AUTHORIZE_TOKEN = "token";
    private static final String AUTHORIZATION = "userId";
    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private AdminIgnoreWhiteProperties adminIgnoreWhiteProperties;
    @Value("${token.expire.seconds}")
    private long timeOut;

    @Override
    public int getOrder() {
        return -3;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String userId = "LOGIN";
        String systemType = SystemTypeEnum.INSURANCE.getName();
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        String path = request.getPath().pathWithinApplication().value();
        String preUuid = UUID.randomUUID().toString();
        String preOutIp = request.getRemoteAddress().getHostString();
        MediaType contentType = request.getHeaders().getContentType();
        boolean filterHeader = false;
        log.info("OMS网关请求路径|path:{}|uuid:{}|ip:{}|method:{}|contentType:{}", path, preUuid, preOutIp, request.getMethod(), contentType);
        if (InsuranceStringUtils.matches(path, adminIgnoreWhiteProperties.getWhites())) {
            filterHeader = true;
        }
        //放行
        if (InsuranceStringUtils.matches(path, "open/")) {
            return chain.filter(exchange);
        }
        GatewayContext gatewayContext = new GatewayContext();
        gatewayContext.setPath(path);
        exchange.getAttributes().put(GatewayContext.CACHE_GATEWAY_CONTEXT, gatewayContext);
        URI uri = exchange.getRequest().getURI();
        HttpHeaders headers = new HttpHeaders();
        headers.putAll(request.getHeaders());
        headers.remove(HttpHeaders.CONTENT_LENGTH);
        if (!filterHeader) {
            //验证token
            String token = headers.getFirst(AUTHORIZE_TOKEN);
            if (StringUtils.isBlank(token)) {
                token = request.getQueryParams().getFirst(AUTHORIZE_TOKEN);
            }
            if (StringUtils.isBlank(token)) {
                log.warn("token不存在:{}", token);
                return GatewayResponseUtils.writeResponse(response, ExtraCodeEnum.INVALID_TOKEN);
            }
            String redisTokenKey = String.format(RedisKeys.OMS_LOGIN_USER_TOKEN, systemType, token);
            if (!redisTemplate.hasKey(redisTokenKey)) {
                log.warn("token2不存在:{}", token);
                return GatewayResponseUtils.writeResponse(response, ExtraCodeEnum.INVALID_TOKEN);
            }
            //验证AUTHORIZATION
            userId = headers.getFirst(AUTHORIZATION);
            if (StringUtils.isBlank(userId)) {
                userId = request.getQueryParams().getFirst(AUTHORIZATION);
            }
            if (StringUtils.isBlank(userId)) {
                return GatewayResponseUtils.writeResponse(response, ExtraCodeEnum.INVALID_TOKEN.code, "USER_ID不存在");
            }
            String redisUserId = redisTemplate.opsForValue().get(redisTokenKey);
            if (StringUtils.isBlank(redisUserId) || !userId.equals(redisUserId)) {
                return GatewayResponseUtils.writeResponse(response, ExtraCodeEnum.INVALID_TOKEN.code, "TOKEN和USER_ID信息不一致");
            }
            String redisUser = String.format(RedisKeys.OMS_LOGIN_USER_ID, systemType, userId);
            redisTemplate.expire(redisUser, timeOut, TimeUnit.MINUTES);
            redisTemplate.expire(redisTokenKey, timeOut, TimeUnit.MINUTES);
        }
        //如果是上传流的方式直接过滤
        if (MediaType.MULTIPART_FORM_DATA.includes(request.getHeaders().getContentType())) {
            return chain.filter(exchange);
        }

        ServerRequest serverRequest = ServerRequest.create(exchange, HandlerStrategies.withDefaults().messageReaders());
        Mono<String> modifiedBody;
        if (HttpMethod.GET.equals(request.getMethod())) {
            String newBody = getPlaintText(null, request, userId, systemType);
            try {
                URI newUri = UriComponentsBuilder.fromUri(uri)
                        .replaceQuery(newBody)
                        .build(true)
                        .toUri();
                ServerHttpRequest newRequest = exchange.getRequest().mutate().uri(newUri).build();
                return chain.filter(exchange.mutate().request(newRequest).response(exchange.getResponse()).build());
            } catch (RuntimeException ex) {
                log.error("请求网关错误|GET|" + preUuid + "|" + preOutIp + "|" + newBody, ex);
                return GatewayResponseUtils.writeResponse(response, ExtraCodeEnum.INVALID_REQUEST);
            }
        }
        if (MediaType.APPLICATION_FORM_URLENCODED.includes(request.getHeaders().getContentType())) {
            return validPostWWWFormData(exchange, chain, gatewayContext, userId, systemType);
        }
        if (MediaType.APPLICATION_JSON.includes(request.getHeaders().getContentType())) {
            final String userIdFinal = userId;
            final String finalSystemType = systemType;
            modifiedBody = serverRequest.bodyToMono(byte[].class)
                    .flatMap(body -> {
                        //解密得到原文,并签名
                        String newBody = getPlaintText(body, request, userIdFinal, finalSystemType);
                        return Mono.just(newBody);
                    });
        } else {
            return GatewayResponseUtils.writeResponse(response, ExtraCodeEnum.INVALID_REQUEST.code, "请求类型不支持");
        }
        BodyInserter bodyInserter = BodyInserters.fromPublisher(modifiedBody, String.class);
        CachedBodyOutputMessage outputMessage = new CachedBodyOutputMessage(exchange, headers);
        Mono mono = bodyInserter.insert(outputMessage, new BodyInserterContext())
                .then(Mono.defer(() -> {
                    ServerHttpRequestDecorator newRequest = getNewRequest(headers, exchange, outputMessage);
                    ServerHttpResponseDecorator newResponse = getNewResponse(exchange.getResponse());
                    return chain.filter(exchange.mutate().request(newRequest).response(newResponse).build());
                }));
        return mono;
    }

    public ServerHttpResponseDecorator getNewResponse(ServerHttpResponse response) {
        DataBufferFactory bufferFactory = response.bufferFactory();
        return new ServerHttpResponseDecorator(response) {
            @Override
            public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                return super.writeWith(DataBufferUtils.join(Flux.from(body))
                        .map(dataBuffer -> {
                            byte[] content = new byte[dataBuffer.readableByteCount()];
                            dataBuffer.read(content);
                            DataBufferUtils.release(dataBuffer);
                            return content;
                        }).flatMap(bytes -> {
                            String bodyString = new String(bytes, StandardCharsets.UTF_8);
                            RespResult result = JacksonUtils.toObj(bodyString, RespResult.class);
                            if (!result.result()) {
                                String originErrorMsg = result.getErrorMsg();
                                if (StringUtils.isNotBlank(originErrorMsg) && originErrorMsg.startsWith("GLOBALEXCEPTION_")) {
                                    String errorMsg = originErrorMsg.substring(originErrorMsg.indexOf(",") + 1); //GLOBALEXCEPTION_ip,
                                    result.setErrorMsg(errorMsg);
                                    log.info("将全局异常消息进行转换，errorCode:{}|originErrorMsg:{}", result.getErrorCode(), originErrorMsg);
                                }
                            }
                            bodyString = JacksonUtils.toJson(result);
                            return Mono.just(bufferFactory.wrap(bodyString.getBytes()));
                        }));
            }
        };
    }

    /**
     * 获取请求原文(GET,和JSON处理方式)
     *
     * @param body
     * @param request
     * @return
     */
    private String getPlaintText(byte[] body, ServerHttpRequest request, String currentUserId, String systemType) {
        if (HttpMethod.GET.equals(request.getMethod())) {
            return validGet(request, currentUserId, systemType);
        } else if (HttpMethod.POST.equals(request.getMethod()) && MediaType.APPLICATION_JSON.includes(request.getHeaders().getContentType())) {
            return validPostJson(request, body, currentUserId, systemType);
        } else {
            return body != null ? new String(body) : "";
        }
    }

    private String validPostJson(ServerHttpRequest request, byte[] body, String currentUserId, String systemType) {
        try {
            String content = new String(body, 0, body.length, "utf-8");
            Map map = JacksonUtils.toObj(content, Map.class);
            //JacksonUtils.toJson(map);
            return validMap(request, map, MediaType.APPLICATION_JSON, currentUserId, systemType);
        } catch (UnsupportedEncodingException e) {
            log.error("解析参数错误", e);
            throw new GwInsuranceException(ExtraCodeEnum.ERROR_REQUEST_PARAM);
        }
    }

    /**
     * 验签GET请求
     *
     * @param request
     * @return
     */
    private String validGet(ServerHttpRequest request, String currentUserId, String systemType) {
        MultiValueMap<String, String> queryParams = request.getQueryParams();
        return validMap(request, queryParams, currentUserId, systemType);
    }


    private String validMap(ServerHttpRequest request, MultiValueMap<String, String> map, String currentUserId, String systemType) {
        Map<String, Object> hashMap = new HashMap<>();
        for (Map.Entry entry : map.entrySet()) {
            hashMap.put(entry.getKey().toString(), ((List) entry.getValue()).get(0).toString());
        }
        return validMap(request, hashMap, MediaType.APPLICATION_FORM_URLENCODED, currentUserId, systemType);
    }

    private String validMap(ServerHttpRequest request, Map<String, Object> map, MediaType mediaType, String currentUserId, String systemType) {
        String sign = MapUtil.getStr(map, "sign");
        if (StringUtils.isBlank(sign)) {
            throw new GwInsuranceException(ExtraCodeEnum.ERROR_PARAMS.code, "缺少sign签名值");
        }
        String signTime = MapUtil.getStr(map, "sign_timestamp");
        if (StringUtils.isBlank(signTime)) {
            throw new GwInsuranceException(ExtraCodeEnum.ERROR_PARAMS.code, "缺少sign_timestamp时间戳");
        }
        Date nowDate = new Date();
        //如果该时间戳超过5分钟，表示请求无效 todo 请求过期、重复请求暂时注释
//        if (nowDate.getTime() - Long.parseLong(signTime) > 5 * 60000) {
//            throw new GwInsuranceException(ExtraCodeEnum.INVALID_REQUEST.code, "请求已过期");
//        }
//        String userSignKey = String.format(RedisKeys.OMS_REQUEST_SIGN_USER_ID, systemType, request.getURI(), sign);
//        if (redisTemplate.hasKey(userSignKey)) {
//            throw new GwInsuranceException(ExtraCodeEnum.INVALID_REQUEST.code, "禁止重复请求");
//        }
//        redisTemplate.opsForValue().set(userSignKey, currentUserId, 5, TimeUnit.MINUTES);
        List<String> keyList = new ArrayList<>();
        for (Map.Entry entry : map.entrySet()) {
            if ("sign".equals(entry.getKey().toString())) {
                continue;
            }
            keyList.add(entry.getKey().toString());
        }
        Collections.sort(keyList);
        List<String> newKey = new ArrayList<>();
        for (int i = 1; i <= keyList.size(); i++) {
            if (i % 2 == 0) {
                newKey.add(keyList.get(i - 1));
                newKey.add(keyList.get(i - 2));
            }
        }
        if (keyList.size() != 0 && keyList.size() % 2 != 0) {
            newKey.add(keyList.get(keyList.size() - 1));
        }
        StringBuilder signPlant = new StringBuilder();
        for (String key : newKey) {
            if (map.get(key) instanceof ArrayList || map.get(key) instanceof LinkedHashMap) {
                signPlant.append("&").append(key).append("=").append(JacksonUtils.toJson(map.get(key)));
            } else {
                Object o = map.get(key);
                signPlant.append("&").append(key).append("=");
                if (!(o == null || "undefiend".equals(o.toString()))) {
                    signPlant.append(map.get(key));
                }
            }
        }
        String s = signPlant.subSequence(1, signPlant.length()).toString();
        boolean verify = SoftSM3Util.verify(s, sign);
        if (!verify) {
            throw new GwInsuranceException(ExtraCodeEnum.AUTH_SIGN_IVALID);
        }
        if (MediaType.APPLICATION_JSON.includes(mediaType)) {
            return JacksonUtils.toJson(map);
        } else {
            return s;
        }
    }

    /**
     * 验签POST请求 www-form
     *
     * @param exchange
     * @param chain
     * @param gatewayContext
     * @return
     */
    private Mono<Void> validPostWWWFormData(ServerWebExchange exchange, GatewayFilterChain chain, GatewayContext gatewayContext, String currentUserId, String sustemType) {
        HttpHeaders headers = exchange.getRequest().getHeaders();
        return exchange.getFormData()
                .doOnNext(multiValueMap -> {
                    gatewayContext.setFormData(multiValueMap);
                    log.debug("[GatewayContext]Read FormData:{}", multiValueMap);
                })
                .then(Mono.defer(() -> {
                    Charset charset = headers.getContentType().getCharset();
                    charset = charset == null ? StandardCharsets.UTF_8 : charset;
                    MultiValueMap<String, String> formData = gatewayContext.getFormData();
                    if (null == formData || formData.isEmpty()) {
                        GatewayResponseUtils.writeResponse(exchange.getResponse(), ExtraCodeEnum.ERROR_REQUEST_PARAM);
                    }
                    String newString = validMap(exchange.getRequest(), formData, currentUserId, sustemType);
                    byte[] bodyBytes = newString.getBytes(charset);
                    int contentLength = bodyBytes.length;
                    ServerHttpRequestDecorator decorator = new ServerHttpRequestDecorator(exchange.getRequest()) {
                        @Override
                        public HttpHeaders getHeaders() {
                            HttpHeaders httpHeaders = new HttpHeaders();
                            httpHeaders.putAll(super.getHeaders());
                            if (contentLength > 0) {
                                httpHeaders.setContentLength(contentLength);
                            } else {
                                httpHeaders.set(HttpHeaders.TRANSFER_ENCODING, "chunked");
                            }
                            return httpHeaders;
                        }

                        @Override
                        public Flux<DataBuffer> getBody() {
                            return DataBufferUtils.read(new ByteArrayResource(bodyBytes), new NettyDataBufferFactory(ByteBufAllocator.DEFAULT), contentLength);
                        }
                    };
                    ServerWebExchange mutateExchange;
                    mutateExchange = exchange.mutate().request(decorator).build();
                    log.info("[GatewayContext]Rewrite Form Data :{}", newString);
                    return chain.filter(mutateExchange);
                }));
    }

    public ServerHttpRequestDecorator getNewRequest(HttpHeaders headers, ServerWebExchange exchange, CachedBodyOutputMessage outputMessage) {
        ServerHttpRequestDecorator decorator = new ServerHttpRequestDecorator(exchange.getRequest()) {
            @Override
            public HttpHeaders getHeaders() {
                long contentLength = headers.getContentLength();
                HttpHeaders httpHeaders = new HttpHeaders();
                httpHeaders.putAll(super.getHeaders());
                if (contentLength > 0) {
                    httpHeaders.setContentLength(contentLength);
                } else {
                    httpHeaders.set(HttpHeaders.TRANSFER_ENCODING, "chunked");
                }
                return httpHeaders;
            }

            @Override
            public Flux<DataBuffer> getBody() {
                return outputMessage.getBody();
            }
        };
        return decorator;
    }
}