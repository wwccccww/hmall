package com.hmall.promotion.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("mq_idempotent_consume")
public class MqIdempotentConsume {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String bizType;
    private String messageId;
    private LocalDateTime createdAt;
}
