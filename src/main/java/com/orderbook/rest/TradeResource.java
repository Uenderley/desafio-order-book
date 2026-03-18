package com.orderbook.rest;

import com.orderbook.dto.PageResponse;
import com.orderbook.entity.Trade;
import com.orderbook.repository.TradeRepository;
import com.orderbook.service.TradeService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

@Path("/api/trades")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Trades", description = "Consulta de trades executados no Order Book")
public class TradeResource {

    @Inject
    TradeService tradeService;

    @Inject
    TradeRepository tradeRepository;

    @GET
    @Operation(summary = "Listar trades", description = "Retorna os trades executados com paginacao, do mais recente ao mais antigo")
    @APIResponse(responseCode = "200", description = "Lista paginada de trades")
    public Response listTrades(
            @Parameter(description = "Numero da pagina (zero-indexed)") @QueryParam("page") @DefaultValue("0") int page,
            @Parameter(description = "Itens por pagina") @QueryParam("size") @DefaultValue("20") int size) {
        List<Trade> content = tradeService.listTrades(page, size);
        long total = tradeRepository.count();
        return Response.ok(new PageResponse<>(content, page, size, total)).build();
    }
}
