package org.actus.risksrv3.repository;

import org.actus.risksrv3.models.BufferLTVModelData;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * BufferLTVModelStore
 *
 * MongoDB repository for BufferLTVModel persistence.
 * Provides standard CRUD operations for buffer-first LTV models.
 */
public interface BufferLTVModelStore extends MongoRepository<BufferLTVModelData, String> {
    // Inherits standard methods:
    // - save(BufferLTVModelData)
    // - findById(String)
    // - findAll()
    // - deleteById(String)
    // - count()
    // - exists(String)
}
