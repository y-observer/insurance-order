package cn.net.insurance.order.common.feign;

import cn.net.insurance.core.base.model.RespResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@Component
@FeignClient(name = "insurance-order-outside-connector")
public interface InsuranceOrderOutsideFeign {
    /**
     * 同步理赔状态
     *
     * @param params
     * @return
     */
    @PostMapping(value = "/outside/invokerCompensate/compensate/syncStatus")
    RespResult syncStatus(@RequestBody Map params);
}
