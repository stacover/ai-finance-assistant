package com.jh.financeResearchAgent.tool.market;

import org.springframework.stereotype.Service;

import java.net.SocketTimeoutException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author jinhang
 * @since 2026/9/15 22:04
 */
@Service
public class MockMarketDataService implements MarketDataService {
  private final AtomicInteger capitalFlowCounter = new AtomicInteger();

  @Override
  public SectorQuote getSectorQuote(String sector) {

    return new SectorQuote(sector, -3.28, 85_600_000_000L, 31.5);
  }

  @Override
  public SectorCapitalFlow getSectorCapitalFlow(String sector) throws Exception {

    int attempt = capitalFlowCounter.incrementAndGet();

    if (attempt <= 2) {

      throw new SocketTimeoutException("mock capital flow timeout");
    }

    return new SectorCapitalFlow(sector, -3_250_000_000L, -1_180_000_000L);
  }

  @Override
  public SectorTopStocks getSectorTopStocks(String sector) {

    return new SectorTopStocks(
        sector,
        List.of(
            new StockPerformance("中芯国际", -4.1),
            new StockPerformance("寒武纪", -6.2),
            new StockPerformance("北方华创", -3.5)));
  }
}
