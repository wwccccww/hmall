package com.hmall.item.domain.po;

import cn.hutool.core.bean.BeanUtil;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class ItemDoc {
    private Long id;
    private String name;
    private Integer price;
    private String image;
    private String category;
    private String brand;
    private Integer sold;
    private Integer commentCount;
    private Boolean isAD;
    private LocalDateTime updateTime;

    public ItemDoc(Item item) {
        BeanUtil.copyProperties(item, this);
    }
}
