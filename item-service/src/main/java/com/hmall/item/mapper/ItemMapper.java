package com.hmall.item.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.hmall.item.domain.dto.OrderDetailDTO;
import com.hmall.item.domain.po.Item;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * <p>
 * 商品表 Mapper 接口
 * </p>
 */
public interface ItemMapper extends BaseMapper<Item> {

    @Update("UPDATE item SET stock = stock - #{num} WHERE id = #{itemId}")
    void updateStock(OrderDetailDTO orderDetail);

    @Select("SELECT DISTINCT category " +
            "FROM item " +
            "WHERE category IS NOT NULL " +
            "  AND category <> '' " +
            "  AND status <> 3 " +
            "ORDER BY category")
    List<String> queryDistinctCategories();
}
