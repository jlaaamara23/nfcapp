package com.nfcattendance.repository;

import com.nfcattendance.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByUsername(String username);
    Optional<UserAccount> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    @Query(value = "select exists (select 1 from user_roles ur join roles r on r.id = ur.role_id where r.name = 'ADMIN')", nativeQuery = true)
    boolean existsAdmin();
}
