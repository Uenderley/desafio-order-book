package com.orderbook.rest;

import com.orderbook.dto.CreateUserRequest;
import com.orderbook.dto.DepositRequest;
import com.orderbook.dto.PageResponse;
import com.orderbook.dto.UserWithWalletResponse;
import com.orderbook.entity.TransactionHistory;
import com.orderbook.entity.User;
import com.orderbook.entity.Wallet;
import com.orderbook.repository.TransactionHistoryRepository;
import com.orderbook.service.UserService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;
import java.util.UUID;

@Path("/api/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Usuarios", description = "Cadastro de usuarios e consulta de wallets e historico")
public class UserResource {

    @Inject
    UserService userService;

    @Inject
    TransactionHistoryRepository txnRepository;

    @GET
    @Operation(summary = "Listar usuarios", description = "Retorna todos os usuarios cadastrados com wallet (saldos disponiveis e reservados) e paginacao")
    @APIResponse(responseCode = "200", description = "Lista paginada de usuarios com wallets")
    public Response listUsers(
            @Parameter(description = "Numero da pagina (zero-indexed)") @QueryParam("page") @DefaultValue("0") int page,
            @Parameter(description = "Itens por pagina") @QueryParam("size") @DefaultValue("20") int size) {
        List<UserWithWalletResponse> content = userService.listUsersWithWallet(page, size);
        long total = userService.countUsers();
        return Response.ok(new PageResponse<>(content, page, size, total)).build();
    }

    @POST
    @Operation(summary = "Criar usuario", description = "Cria um novo usuario com wallet e saldos iniciais de BRL e Vibranium")
    @APIResponse(responseCode = "201", description = "Usuario criado com sucesso")
    public Response createUser(CreateUserRequest request) {
        User user = userService.createUser(
                request.name, request.email,
                request.initialBalanceBrl, request.initialBalanceVibranium);
        return Response.status(201).entity(user).build();
    }

    @GET
    @Path("/{id}/wallet")
    @Operation(summary = "Consultar wallet", description = "Retorna saldos disponiveis e reservados de BRL e Vibranium")
    @APIResponse(responseCode = "200", description = "Wallet do usuario")
    @APIResponse(responseCode = "404", description = "Usuario nao encontrado")
    public Response getWallet(
            @Parameter(description = "ID do usuario") @PathParam("id") UUID userId) {
        Wallet wallet = userService.getWallet(userId);
        return Response.ok(wallet).build();
    }

    @PATCH
    @Path("/{id}/wallet/deposit")
    @Operation(summary = "Depositar saldo", description = "Adiciona BRL e/ou Vibranium ao saldo disponivel da wallet do usuario")
    @APIResponse(responseCode = "200", description = "Deposito realizado com sucesso")
    @APIResponse(responseCode = "404", description = "Usuario nao encontrado")
    public Response deposit(
            @Parameter(description = "ID do usuario") @PathParam("id") UUID userId,
            DepositRequest request) {
        Wallet wallet = userService.deposit(userId, request.amountBrl, request.amountVibranium);
        return Response.ok(wallet).build();
    }

    @GET
    @Path("/{id}/transactions")
    @Operation(summary = "Historico de transacoes", description = "Lista as transacoes (compras e vendas) do usuario com paginacao")
    @APIResponse(responseCode = "200", description = "Lista paginada de transacoes")
    public Response getTransactions(
            @Parameter(description = "ID do usuario") @PathParam("id") UUID userId,
            @Parameter(description = "Numero da pagina (zero-indexed)") @QueryParam("page") @DefaultValue("0") int page,
            @Parameter(description = "Itens por pagina") @QueryParam("size") @DefaultValue("20") int size) {
        List<TransactionHistory> content = txnRepository.findByUserId(userId, page, size);
        long total = txnRepository.countByUserId(userId);
        return Response.ok(new PageResponse<>(content, page, size, total)).build();
    }
}
