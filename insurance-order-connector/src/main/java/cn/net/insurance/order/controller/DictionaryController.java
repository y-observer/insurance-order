package cn.net.insurance.order.controller;

import cn.net.insurance.core.base.model.RespResult;
import cn.net.insurance.order.common.dto.request.DictionaryReqDto;
import cn.net.insurance.order.entity.Dictionary;
import cn.net.insurance.order.service.DictionaryService;
import io.swagger.annotations.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/dictionary")
@RestController
@Api(tags = "数据字典")
public class DictionaryController {

    @Autowired
    private DictionaryService dictionaryService;

    @PostMapping("/list-page")
    @ApiOperation(value = "查询列表接口",notes = "查询所有的字典信息")
    @ApiImplicitParams({@ApiImplicitParam(name = "typeCode",value = "字典类型编码")})
    public RespResult<List<Dictionary>> listPage(@RequestBody DictionaryReqDto dictionaryReqDto) {
        List<Dictionary> dictionaries = dictionaryService.queryDictionaryByTypeCode(dictionaryReqDto.getTypeCode());
        System.out.println("[dictionary]业务实现---->" + dictionaries.toString());
        return RespResult.success(dictionaries);
    }
}
