package com.jh.financeResearchAgent.tool.market;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * @author jinhang
 * @since 2026/9/2 22:24
 */
@Component
public class MarketTools {
  @Tool(
      name = "getSectorQuote",
      description =
          """
                获取指定行业板块的最新市场行情。
                当用户询问某个行业或板块的涨跌、
                成交额、市场表现时使用该工具。
                """)
  public SectorQuote getSectorQuote(
      @ToolParam(description = "板块名称，例如：半导体、CPO、有色金属") String sector) {
    // 第一阶段先 Mock 外部行情源
    return new SectorQuote(sector, -3.28, 85_600_000_000L, 31.5);
  }
}
