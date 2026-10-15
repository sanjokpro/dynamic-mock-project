package com.dynamicmock.infrastructure.persistence.mongodb.repository;

import com.dynamicmock.domain.entity.User;
import com.dynamicmock.domain.port.out.UserRepository;
import com.dynamicmock.infrastructure.persistence.mongodb.entity.UserMongoEntity;
import com.dynamicmock.infrastructure.persistence.mongodb.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final MongoUserRepository mongoRepository;
    private final UserMapper mapper;

    @Override
    public Optional<User> findById(String id) {
        return mongoRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return mongoRepository.findByUsername(username).map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return mongoRepository.findByEmail(email).map(mapper::toDomain);
    }

    @Override
    public boolean existsByUsername(String username) {
        return mongoRepository.existsByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return mongoRepository.existsByEmail(email);
    }

    @Override
    public User save(User user) {
        UserMongoEntity entity = mapper.toEntity(user);
        UserMongoEntity saved = mongoRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public long count() {
        return mongoRepository.count();
    }
}
