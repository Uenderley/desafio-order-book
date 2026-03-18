package com.orderbook.rest;

import com.orderbook.dto.CreateOrderRequest;
import com.orderbook.entity.Order;
import com.orderbook.service.OrderService;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.UUID;

@Path("/api/orders")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Ordens", description = "Criar, consultar e cancelar ordens de compra/venda de Vibranium")
public class OrderResource {

    @Inject
    OrderService orderService;

    @POST
    @Transactional
    @Operation(summary = "Criar ordem", description = "Cria uma ordem de compra (BUY) ou venda (SELL). O saldo necessario e reservado automaticamente. Se houver match, o trade e executado imediatamente.")
    @APIResponse(responseCode = "201", description = "Ordem criada com sucesso")
    @APIResponse(responseCode = "400", description = "Saldo insuficiente ou parametros invalidos")
    public Response createOrder(
            @Parameter(description = "ID do usuario (header)", required = true) @HeaderParam("X-User-Id") UUID userId,
            CreateOrderRequest request) {
        Order order = orderService.createOrder(userId, request.side, request.price, request.quantity);
        return Response.status(201).entity(order).build();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Consultar ordem", description = "Retorna os detalhes de uma ordem pelo ID")
    @APIResponse(responseCode = "200", description = "Ordem encontrada")
    @APIResponse(responseCode = "404", description = "Ordem nao encontrada")
    public Response getOrder(
            @Parameter(description = "ID da ordem") @PathParam("id") UUID orderId) {
        Order order = orderService.getOrder(orderId);
        return Response.ok(order).build();
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    @Operation(summary = "Cancelar ordem", description = "Cancela uma ordem aberta (NEW ou PARTIALLY_FILLED). O saldo reservado e devolvido a wallet.")
    @APIResponse(responseCode = "200", description = "Ordem cancelada")
    @APIResponse(responseCode = "400", description = "Ordem nao pode ser cancelada (ja FILLED ou CANCELLED)")
    @APIResponse(responseCode = "404", description = "Ordem nao encontrada")
    public Response cancelOrder(
            @Parameter(description = "ID do usuario (header)", required = true) @HeaderParam("X-User-Id") UUID userId,
            @Parameter(description = "ID da ordem") @PathParam("id") UUID orderId) {
        Order order = orderService.cancelOrder(userId, orderId);
        return Response.ok(order).build();
    }
}
