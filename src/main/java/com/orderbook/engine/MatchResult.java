package com.orderbook.engine;

import java.util.List;

public record MatchResult(List<TradeResult> trades) {}
