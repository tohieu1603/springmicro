package com.hieu.order_service.infrastructure.persistence.jpa.repositories;

import com.hieu.order_service.infrastructure.persistence.jpa.entities.IdempotencyJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyJpaRepository extends JpaRepository<IdempotencyJpaEntity, String> {}
