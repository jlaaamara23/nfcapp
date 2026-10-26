package com.nfcattendance.repository;

import com.nfcattendance.entity.RoleName;
import com.nfcattendance.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByUsername(String username);
    Optional<UserAccount> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    @Query("select count(u) > 0 from UserAccount u join u.roles r where r.name = :role")
    boolean existsByRole(RoleName role);
}
