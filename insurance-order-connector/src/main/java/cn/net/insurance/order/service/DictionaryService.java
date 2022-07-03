package cn.net.insurance.order.service;


import cn.net.insurance.order.entity.Dictionary;
import java.util.List;

public interface DictionaryService {

    /**
     * 根据类型编码查询数据字典
     * @param typeCode
     * @return
     */
    List<Dictionary> queryDictionaryByTypeCode(String typeCode);
}
