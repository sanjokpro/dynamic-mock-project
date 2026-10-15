package com.dynamicmock.infrastructure.persistence;

import com.dynamicmock.domain.entity.User;
import com.dynamicmock.infrastructure.persistence.mongodb.entity.UserMongoEntity;
import com.dynamicmock.infrastructure.persistence.mongodb.mapper.UserMapper;
import com.dynamicmock.infrastructure.persistence.mongodb.repository.MongoUserRepository;
import com.dynamicmock.infrastructure.persistence.mongodb.repository.UserRepositoryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserRepositoryImplTest {

    @Mock
    private MongoUserRepository mongoRepository;

    private UserMapper mapper;
    private UserRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        mapper = new UserMapper();
        repository = new UserRepositoryImpl(mongoRepository, mapper);
    }

    @Test
    @DisplayName("save should map to entity and back to domain")
    void testSave() {
        User domain = User.builder()
                .id("u1")
                .username("admin")
                .email("admin@test.com")
                .passwordHash("hashed")
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(mongoRepository.save(any(UserMongoEntity.class))).thenAnswer(i -> i.getArgument(0));

        User saved = repository.save(domain);

        assertThat(saved).isNotNull();
        assertThat(saved.getUsername()).isEqualTo("admin");
        assertThat(saved.getEmail()).isEqualTo("admin@test.com");
    }

    @Test
    @DisplayName("findByUsername should return mapped domain User")
    void testFindByUsername() {
        UserMongoEntity entity = UserMongoEntity.builder()
                .id("u1")
                .username("admin")
                .email("admin@test.com")
                .passwordHash("hashed")
                .active(true)
                .build();

        when(mongoRepository.findByUsername("admin")).thenReturn(Optional.of(entity));

        Optional<User> found = repository.findByUsername("admin");

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo("u1");
        assertThat(found.get().getUsername()).isEqualTo("admin");
    }

    @Test
    @DisplayName("count should delegate to mongoRepository.count()")
    void testCount() {
        when(mongoRepository.count()).thenReturn(3L);

        long count = repository.count();

        assertThat(count).isEqualTo(3L);
        verify(mongoRepository).count();
    }
}
