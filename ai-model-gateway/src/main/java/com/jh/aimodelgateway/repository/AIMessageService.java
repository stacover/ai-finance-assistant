package com.jh.aimodelgateway.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jh.aimodelgateway.entity.AIMessage;
import com.jh.aimodelgateway.mapper.AIMessageMapper;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * @author jinhang
 * @since 2026/8/12 21:46
 */
@Service
public class AIMessageService extends ServiceImpl<AIMessageMapper, AIMessage> {

  public List<AIMessage> getByConversationId(String conversationId) {
    LambdaQueryWrapper<AIMessage> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(AIMessage::getConversationId, conversationId);
    wrapper.orderByAsc(AIMessage::getCreateTime);
    return list(wrapper);
  }
}
