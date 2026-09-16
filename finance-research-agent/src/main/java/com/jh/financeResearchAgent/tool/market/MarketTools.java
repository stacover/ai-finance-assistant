package com.jh.financeResearchAgent.tool.market;

import com.jh.financeResearchAgent.tool.ToolResult;
import com.jh.financeResearchAgent.tool.runtime.ToolExecutionPolicy;
import com.jh.financeResearchAgent.tool.runtime.ToolExecutionRuntime;
import java.time.Duration;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * @author jinhang
 * @since 2026/9/2 22:24
 */
@Component
@RequiredArgsConstructor
public class MarketTools {
  private static final ToolExecutionPolicy MARKET_DATA_POLICY =
      new ToolExecutionPolicy(3, Duration.ofSeconds(2), Duration.ofMillis(200));

  private final ToolExecutionRuntime toolExecutionRuntime;

  private final MarketDataService marketDataService;

  @Tool(
      name = "getSectorQuote",
      description =
          """
                    获取指定行业板块的最新行情，
                    包括涨跌幅、成交额等市场数据。
                    """)
  public ToolResult<SectorQuote> getSectorQuote(@ToolParam(description = "板块名称") String sector) {

    return toolExecutionRuntime.invoke(
        "getSectorQuote", MARKET_DATA_POLICY, () -> marketDataService.getSectorQuote(sector));
  }

  @Tool(
      name = "getSectorCapitalFlow",
      description =
          """
                获取指定行业板块的资金流向，
                用于分析资金流入和流出情况。
                """)
  public ToolResult<SectorCapitalFlow> getSectorCapitalFlow(
      @ToolParam(description = "板块名称") String sector) {

    return toolExecutionRuntime.invoke(
        "getSectorCapitalFlow",
        MARKET_DATA_POLICY,
        () -> marketDataService.getSectorCapitalFlow(sector));
  }

  @Tool(
      name = "getSectorTopStocks",
      description =
          """
                     获取指定行业板块主要成分股的当日涨跌情况。
                     当需要分析板块下跌是否由核心股票拖累、
                     是否出现普跌或分化时使用。
                 """)
  public ToolResult<SectorTopStocks> getSectorTopStocks(
      @ToolParam(description = "板块名称，例如：半导体、CPO、有色金属") String sector) {
    return toolExecutionRuntime.invoke(
        "getSectorTopStocks",
        MARKET_DATA_POLICY,
        () -> marketDataService.getSectorTopStocks(sector));
  }
}
