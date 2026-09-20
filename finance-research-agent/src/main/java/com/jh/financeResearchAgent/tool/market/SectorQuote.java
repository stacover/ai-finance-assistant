package com.jh.financeResearchAgent.tool.market;

/**
 * @author jinhang
 * @since 2026/9/2 22:23
 */
public record SectorQuote(
    String sector, double changePercent, long turnover, double turnoverChangePercent) {}
