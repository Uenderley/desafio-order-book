package com.orderbook.rest;

import com.orderbook.engine.MatchingEngine;
import com.orderbook.engine.OrderBookSnapshot;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/orderbook")
@Produces(MediaType.APPLICATION_JSON)
public class OrderBookResource {

    @Inject
    MatchingEngine matchingEngine;

    @GET
    public Response getOrderBook() {
        OrderBookSnapshot snapshot = matchingEngine.getOrderBook();
        return Response.ok(snapshot).build();
    }
}
