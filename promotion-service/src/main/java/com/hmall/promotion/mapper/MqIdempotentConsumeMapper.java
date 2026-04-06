package com.hmall.promotion.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hmall.promotion.domain.po.MqIdempotentConsume;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MqIdempotentConsumeMapper extends BaseMapper<MqIdempotentConsume> {
}
