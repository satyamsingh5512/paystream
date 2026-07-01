package com.paystream.account.controller;

import com.paystream.account.dto.AccountResponse;
import com.paystream.account.dto.BalanceResponse;
import com.paystream.account.dto.CreateAccountRequest;
import com.paystream.account.dto.MoneyOperationRequest;
import com.paystream.account.service.AccountCommandService;
import com.paystream.account.service.AccountQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Account command and query endpoints. */
@RestController
@RequestMapping("/api/v1/accounts")
@Tag(name = "Accounts", description = "Account creation, balances (read model) and debit/credit operations")
public class AccountController {

    private final AccountCommandService commandService;
    private final AccountQueryService queryService;

    public AccountController(AccountCommandService commandService, AccountQueryService queryService) {
        this.commandService = commandService;
        this.queryService = queryService;
    }

    @PostMapping
    @Operation(summary = "Open a new account")
    @ApiResponse(responseCode = "201", description = "Account created")
    public ResponseEntity<AccountResponse> create(@Valid @RequestBody CreateAccountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(AccountResponse.from(commandService.create(request)));
    }

    @GetMapping("/{id}/balance")
    @Operation(summary = "Get an account balance from the read model")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Balance returned"),
            @ApiResponse(responseCode = "404", description = "Account not found")
    })
    public BalanceResponse balance(@PathVariable UUID id) {
        return BalanceResponse.from(queryService.getBalance(id));
    }

    @GetMapping
    @Operation(summary = "List account balances for an owner")
    public List<BalanceResponse> byOwner(@RequestParam UUID ownerId) {
        return queryService.getByOwner(ownerId).stream().map(BalanceResponse::from).toList();
    }

    @PostMapping("/{id}/debit")
    @Operation(summary = "Debit an account (invoked by the transaction saga)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Debited"),
            @ApiResponse(responseCode = "404", description = "Account not found"),
            @ApiResponse(responseCode = "422", description = "Insufficient balance")
    })
    public AccountResponse debit(@PathVariable UUID id, @Valid @RequestBody MoneyOperationRequest request) {
        return AccountResponse.from(commandService.debit(id, request.amount()));
    }

    @PostMapping("/{id}/credit")
    @Operation(summary = "Credit an account (invoked by the transaction saga / compensation)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Credited"),
            @ApiResponse(responseCode = "404", description = "Account not found")
    })
    public AccountResponse credit(@PathVariable UUID id, @Valid @RequestBody MoneyOperationRequest request) {
        return AccountResponse.from(commandService.credit(id, request.amount()));
    }
}
