package com.jh.financeResearchAgent.tool.market;

import java.util.List;

/**
 * @author jinhang
 * @since 2026/9/8 21:39
 */
public record SectorTopStocks(String sector, List<StockPerformance> topStocks) {}
