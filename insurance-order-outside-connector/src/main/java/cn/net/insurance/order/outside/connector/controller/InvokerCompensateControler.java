package cn.net.insurance.order.outside.connector.controller;

import cn.net.insurance.core.base.model.RespResult;
import cn.net.insurance.order.common.constant.ServerUrlConstants;
import cn.net.insurance.order.common.utils.ServiceExchangeUtils;
import com.alibaba.nacos.common.utils.JacksonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/invokerCompensate")
public class InvokerCompensateControler {

    /**
     * 同步理赔状态
     *
     * @param params
     * @return
     */
    @RequestMapping(value = "/compensate/syncStatus")
    public RespResult syncStatus(@RequestBody Map params) {
        System.out.println("调用理赔服务...." );
        RespResult respResult = ServiceExchangeUtils.httpPostJson2ServiceOnMap(ServerUrlConstants.DICTIONARY_LIST_PAGE, params, Void.class);
        return RespResult.success();
    }
}
