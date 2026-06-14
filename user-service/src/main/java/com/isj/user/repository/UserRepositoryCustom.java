package com.isj.user.repository;

import com.isj.user.domain.User;

import java.util.Optional;

public interface UserRepositoryCustom {

    Optional<User> findUserByEmail(String email);

    boolean existsUserByEmail(String email);
}
