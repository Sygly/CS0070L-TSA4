package com.mycompany.bookbridge.service;

import java.util.Optional;

public interface Repository<T, ID> {
    Optional<T> findByKey(ID id);
}