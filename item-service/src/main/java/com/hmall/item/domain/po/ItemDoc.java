package com.hmall.item.domain.po;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
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
    /**
     * 原始规格，可能为 JSON 字符串，如 {"颜色":"银色","尺寸":"28寸"}
     */
    private String spec;
    /**
     * 从 spec JSON 解析出的颜色，便于 ES 精准召回「金色/银色」等
     */
    private String specColor;
    /**
     * 从 spec JSON 解析出的尺寸
     */
    private String specSize;
    private Integer sold;
    private Integer commentCount;
    private Boolean isAD;
    /**
     * 聚合检索字段：用于 matchQuery("all", key)
     */
    private String all;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Long creater;
    private Long updater;

    public ItemDoc(Item item) {
        BeanUtil.copyProperties(item, this);
        fillFromSpecJson(item == null ? null : item.getSpec());
        this.all = buildAll(item);
    }

    private void fillFromSpecJson(String specStr) {
        if (StrUtil.isBlank(specStr)) {
            return;
        }
        String trim = specStr.trim();
        if (!trim.startsWith("{")) {
            return;
        }
        try {
            JSONObject o = JSONUtil.parseObj(trim);
            this.specColor = StrUtil.blankToDefault(o.getStr("颜色"), null);
            this.specSize = StrUtil.blankToDefault(o.getStr("尺寸"), null);
        } catch (Exception ignored) {
            // 非 JSON 或旧数据，保持 spec 原串进 all 即可
        }
    }

    private String buildAll(Item item) {
        if (item == null) {
            return "";
        }
        return StrUtil.join(" ",
                StrUtil.blankToDefault(item.getName(), ""),
                StrUtil.blankToDefault(item.getBrand(), ""),
                StrUtil.blankToDefault(item.getCategory(), ""),
                StrUtil.blankToDefault(item.getSpec(), ""),
                StrUtil.blankToDefault(this.specColor, ""),
                StrUtil.blankToDefault(this.specSize, "")
        ).trim();
    }
}
