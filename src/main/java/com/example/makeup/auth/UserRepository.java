package com.example.makeup.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByAuthSubject(UUID authSubject);
    boolean existsByUsername(String username);
    boolean existsByAuthSubject(UUID authSubject);
}
