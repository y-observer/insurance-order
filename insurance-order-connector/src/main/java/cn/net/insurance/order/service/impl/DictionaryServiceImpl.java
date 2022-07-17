package cn.net.insurance.order.service.impl;

import cn.net.insurance.order.common.feign.InsuranceOrderOutsideFeign;
import cn.net.insurance.order.entity.Dictionary;
import cn.net.insurance.order.mapper.DictionaryMapper;
import cn.net.insurance.order.service.DictionaryService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class DictionaryServiceImpl implements DictionaryService {

    @Autowired
    private DictionaryMapper dictionaryMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private InsuranceOrderOutsideFeign insuranceOrderOutsideFeign;

    @Autowired
    private RocketMQTemplate rocketMQTemplate;
    @Value("${rocket.topic.insurance-order}")
    private String insuranceOrderTopic;

    @Override
    public List<Dictionary> queryDictionaryByTypeCode(String typeCode) {
        Map map= new HashMap();
        map.put("name","李四");
        String res =  redisTemplate.opsForValue().get("oms:insurance:user:userId:-1");
        log.info("[redis]测试-->" + res);
        rocketMQTemplate.convertAndSend(insuranceOrderTopic,"rockeMq测试");
        insuranceOrderOutsideFeign.syncStatus(map);
        return dictionaryMapper.queryDictionaryByTypeCode(typeCode);
    }
}
