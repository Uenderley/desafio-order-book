package com.orderbook.rest;

import com.orderbook.dto.CreateUserRequest;
import com.orderbook.dto.PageResponse;
import com.orderbook.entity.TransactionHistory;
import com.orderbook.entity.User;
import com.orderbook.entity.Wallet;
import com.orderbook.repository.TransactionHistoryRepository;
import com.orderbook.service.UserService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.UUID;

@Path("/api/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserResource {

    @Inject
    UserService userService;

    @Inject
    TransactionHistoryRepository txnRepository;

    @POST
    public Response createUser(CreateUserRequest request) {
        User user = userService.createUser(
                request.name, request.email,
                request.initialBalanceBrl, request.initialBalanceVibranium);
        return Response.status(201).entity(user).build();
    }

    @GET
    @Path("/{id}/wallet")
    public Response getWallet(@PathParam("id") UUID userId) {
        Wallet wallet = userService.getWallet(userId);
        return Response.ok(wallet).build();
    }

    @GET
    @Path("/{id}/transactions")
    public Response getTransactions(
            @PathParam("id") UUID userId,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        List<TransactionHistory> content = txnRepository.findByUserId(userId, page, size);
        long total = txnRepository.countByUserId(userId);
        return Response.ok(new PageResponse<>(content, page, size, total)).build();
    }
}
