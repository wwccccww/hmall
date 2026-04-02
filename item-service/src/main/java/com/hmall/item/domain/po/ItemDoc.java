package com.hmall.item.domain.po;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class ItemDoc {
    private Long id;
    private String name;
    private Integer price;
    private Integer stock;
    private String image;
    private String category;
    private String brand;
    private String spec;
    private Integer sold;
    private Integer commentCount;
    private Boolean isAD;
    /**
     * 聚合检索字段：用于 matchQuery("all", key)
     * 通常由 name/brand/category/spec 拼接而来。
     */
    private String all;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Long creater;
    private Long updater;

    public ItemDoc(Item item) {
        BeanUtil.copyProperties(item, this);
        this.all = buildAll(item);
    }

    private static String buildAll(Item item) {
        if (item == null) {
            return "";
        }
        // 用空格拼接，便于分词与召回；为空的字段会被忽略
        return StrUtil.join(" ",
                StrUtil.blankToDefault(item.getName(), ""),
                StrUtil.blankToDefault(item.getBrand(), ""),
                StrUtil.blankToDefault(item.getCategory(), ""),
                StrUtil.blankToDefault(item.getSpec(), "")
        ).trim();
    }
}
