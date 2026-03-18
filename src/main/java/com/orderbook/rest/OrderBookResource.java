package com.orderbook.rest;

import com.orderbook.engine.MatchingEngine;
import com.orderbook.engine.OrderBookSnapshot;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

@Path("/api/orderbook")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Order Book", description = "Visualizacao do livro de ofertas (bids e asks)")
public class OrderBookResource {

    @Inject
    MatchingEngine matchingEngine;

    @GET
    @Operation(summary = "Livro de ofertas", description = "Retorna o snapshot atual do Order Book. Use status=OPEN para filtrar apenas ordens com status NEW ou PARTIALLY_FILLED.")
    @APIResponse(responseCode = "200", description = "Snapshot do Order Book")
    public Response getOrderBook(
            @Parameter(description = "Filtro por status: OPEN (NEW + PARTIALLY_FILLED), NEW, PARTIALLY_FILLED. Sem filtro retorna todas.")
            @QueryParam("status") String status) {
        OrderBookSnapshot snapshot = matchingEngine.getOrderBook();

        if (status != null && !status.isBlank()) {
            snapshot = filterByStatus(snapshot, status.toUpperCase());
        }

        return Response.ok(snapshot).build();
    }

    private OrderBookSnapshot filterByStatus(OrderBookSnapshot snapshot, String status) {
        List<OrderBookSnapshot.Entry> filteredBids = snapshot.bids().stream()
                .filter(e -> matchesStatus(e.status(), status))
                .toList();
        List<OrderBookSnapshot.Entry> filteredAsks = snapshot.asks().stream()
                .filter(e -> matchesStatus(e.status(), status))
                .toList();
        return new OrderBookSnapshot(filteredBids, filteredAsks);
    }

    private boolean matchesStatus(String entryStatus, String filter) {
        if ("OPEN".equals(filter)) {
            return "NEW".equals(entryStatus) || "PARTIALLY_FILLED".equals(entryStatus);
        }
        return filter.equals(entryStatus);
    }
}
