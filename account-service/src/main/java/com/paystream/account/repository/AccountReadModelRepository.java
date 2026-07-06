package com.paystream.account.repository;

import com.paystream.account.domain.AccountReadModel;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Read-model data access for account balances. */
public interface AccountReadModelRepository extends JpaRepository<AccountReadModel, UUID> {

    List<AccountReadModel> findByOwnerId(UUID ownerId);
}
