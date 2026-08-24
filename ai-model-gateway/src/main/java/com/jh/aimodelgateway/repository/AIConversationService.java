package com.jh.aimodelgateway.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jh.aimodelgateway.entity.AIConversation;
import com.jh.aimodelgateway.mapper.AIConversationMapper;
import org.springframework.stereotype.Service;

/**
 * @author jinhang
 * @since 2026/8/12 21:45
 */
@Service
public class AIConversationService extends ServiceImpl<AIConversationMapper, AIConversation> {

  public AIConversation getByConversationId(String conversationId) {
    LambdaQueryWrapper<AIConversation> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(AIConversation::getConversationId, conversationId);
    wrapper.last("limit 1");
    return getOne(wrapper);
  }
}
