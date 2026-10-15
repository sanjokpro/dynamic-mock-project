package com.dynamicmock.domain.port.out;

import com.dynamicmock.domain.entity.User;
import java.util.Optional;

/**
 * Output port for User persistence operations.
 * Clean Architecture interface decoupled from infrastructure implementation.
 */
public interface UserRepository {
    Optional<User> findById(String id);
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    User save(User user);
    long count();
}
