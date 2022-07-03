package cn.net.insurance.order.entity;

import cn.net.insurance.core.mybatis.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName(value = "dictionary")
public class Dictionary extends BaseEntity {

    /**
     * 类型编号
     */
    @TableField("type_code")
    private String typeCode;

    /**
     * 类型名称
     */
    @TableField("type_name")
    private String typeName;

    /**
     * 字典编号
     */
    @TableField("dictionary_code")
    private String dictionaryCode;

    /**
     * 字典名称
     */
    @TableField("dictionary_name")
    private String dictionaryName;


    /**
     * 排序
     */
    @TableField("sort")
    private Integer sort;


}