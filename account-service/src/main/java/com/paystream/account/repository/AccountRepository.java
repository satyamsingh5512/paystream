package com.paystream.account.repository;

import com.paystream.account.domain.Account;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Write-model data access for {@link Account}. */
public interface AccountRepository extends JpaRepository<Account, UUID> {
}
