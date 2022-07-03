package cn.net.insurance.order.gateway.utils;

import cn.net.insurance.core.base.model.ExtraCodeEnum;
import cn.net.insurance.core.base.model.RespResult;
import com.alibaba.nacos.common.utils.JacksonUtils;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import reactor.core.publisher.Mono;


public class GatewayResponseUtils {
    public static Mono<Void> writeResponse(ServerHttpResponse response, String code, String msg) {
        HttpHeaders httpHeaders = response.getHeaders();
        //返回数据格式
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        return response.writeWith(Mono.fromSupplier(() -> {
            DataBufferFactory bufferFactory = response.bufferFactory();
            byte[] bytes = JacksonUtils.toJsonBytes(RespResult.fail(code, msg));
            return bufferFactory.wrap(bytes);
        }));
    }

    public static Mono<Void> writeResponse(ServerHttpResponse response, ExtraCodeEnum respError) {
        return writeResponse(response, respError.getCode(), respError.getMsg());
    }
}
