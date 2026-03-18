package com.orderbook.rest;

import com.orderbook.dto.CreateOrderRequest;
import com.orderbook.entity.Order;
import com.orderbook.service.OrderService;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.UUID;

@Path("/api/orders")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class OrderResource {

    @Inject
    OrderService orderService;

    @POST
    @Transactional
    public Response createOrder(
            @HeaderParam("X-User-Id") UUID userId,
            CreateOrderRequest request) {
        Order order = orderService.createOrder(userId, request.side, request.price, request.quantity);
        return Response.status(201).entity(order).build();
    }

    @GET
    @Path("/{id}")
    public Response getOrder(@PathParam("id") UUID orderId) {
        Order order = orderService.getOrder(orderId);
        return Response.ok(order).build();
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    public Response cancelOrder(
            @HeaderParam("X-User-Id") UUID userId,
            @PathParam("id") UUID orderId) {
        Order order = orderService.cancelOrder(userId, orderId);
        return Response.ok(order).build();
    }
}
