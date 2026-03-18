package com.orderbook.rest;

import com.orderbook.dto.PageResponse;
import com.orderbook.entity.Trade;
import com.orderbook.repository.TradeRepository;
import com.orderbook.service.TradeService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@Path("/api/trades")
@Produces(MediaType.APPLICATION_JSON)
public class TradeResource {

    @Inject
    TradeService tradeService;

    @Inject
    TradeRepository tradeRepository;

    @GET
    public Response listTrades(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        List<Trade> content = tradeService.listTrades(page, size);
        long total = tradeRepository.count();
        return Response.ok(new PageResponse<>(content, page, size, total)).build();
    }
}
