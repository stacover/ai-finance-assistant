package com.jh.financeResearchAgent.tool.market;

import java.util.List;
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

  @Tool(
      name = "getSectorCapitalFlow",
      description =
          """
          获取指定行业板块的资金流向数据。
          当需要判断板块上涨或下跌过程中是否存在
          主力资金流入，流出时使用
          """)
  public SectorCapitalFlow getSectorCapitalFlow(
      @ToolParam(description = "板块名称，例如：半导体、CPO、有色金属") String sector) {
    return new SectorCapitalFlow(sector, -3_250_000_000L, -1_180_000_000L);
  }

  @Tool(
      name = "getSectorTopStocks",
      description =
          """
             获取指定行业板块主要成分股的当日涨跌情况。
             当需要分析板块下跌是否由核心股票拖累、
             是否出现普跌或分化时使用。
         """)
  public SectorTopStocks getSectorTopStocks(
      @ToolParam(description = "板块名称，例如：半导体、CPO、有色金属") String sector) {
    return new SectorTopStocks(
        sector,
        List.of(
            new StockPerformance("中芯国际", -4.1),
            new StockPerformance("寒武纪", -6.2),
            new StockPerformance("北方华创", -3.5)));
  }
}
