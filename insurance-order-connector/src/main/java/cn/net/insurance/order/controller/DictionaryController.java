package cn.net.insurance.order.controller;

import cn.net.insurance.order.entity.Dictionary;
import cn.net.insurance.order.service.DictionaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequestMapping("/dictionary")
@RestController
public class DictionaryController {

    @Autowired
    private DictionaryService dictionaryService;

    @PostMapping("/list-page")
    public void listPage() {
        List<Dictionary> dictionaries = dictionaryService.queryDictionaryByTypeCode("company_type");
        System.out.println("[dictionary]业务实现---->" + dictionaries.toString());
    }
}
