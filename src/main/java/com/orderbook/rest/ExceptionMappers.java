package com.orderbook.rest;

import com.orderbook.dto.ErrorResponse;
import com.orderbook.exception.InsufficientBalanceException;
import com.orderbook.exception.OrderNotFoundException;
import com.orderbook.exception.UserNotFoundException;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

public class ExceptionMappers {

    @ServerExceptionMapper(InsufficientBalanceException.class)
    public Response handleInsufficientBalance(InsufficientBalanceException e) {
        return Response.status(400).entity(new ErrorResponse(e.getMessage())).build();
    }

    @ServerExceptionMapper(OrderNotFoundException.class)
    public Response handleOrderNotFound(OrderNotFoundException e) {
        return Response.status(404).entity(new ErrorResponse(e.getMessage())).build();
    }

    @ServerExceptionMapper(UserNotFoundException.class)
    public Response handleUserNotFound(UserNotFoundException e) {
        return Response.status(404).entity(new ErrorResponse(e.getMessage())).build();
    }

    @ServerExceptionMapper(IllegalArgumentException.class)
    public Response handleIllegalArgument(IllegalArgumentException e) {
        return Response.status(400).entity(new ErrorResponse(e.getMessage())).build();
    }

    @ServerExceptionMapper(IllegalStateException.class)
    public Response handleIllegalState(IllegalStateException e) {
        return Response.status(400).entity(new ErrorResponse(e.getMessage())).build();
    }
}
