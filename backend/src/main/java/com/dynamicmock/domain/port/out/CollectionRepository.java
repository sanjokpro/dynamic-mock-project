package com.dynamicmock.domain.port.out;

import com.dynamicmock.domain.entity.Collection;
import java.util.List;
import java.util.Optional;

public interface CollectionRepository {
    Collection save(Collection collection);
    Optional<Collection> findById(String id);
    List<Collection> findByUserId(String userId);
    void deleteById(String id);
}
