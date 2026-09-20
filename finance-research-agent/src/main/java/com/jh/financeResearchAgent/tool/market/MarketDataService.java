package com.jh.financeResearchAgent.tool.market;

/**
 * @author jinhang
 * @since 2026/9/15 22:04
 */
public interface MarketDataService {

    SectorQuote getSectorQuote(String sector)
            throws Exception;

    SectorCapitalFlow getSectorCapitalFlow(String sector)
            throws Exception;

    SectorTopStocks getSectorTopStocks(String sector)
            throws Exception;
}
