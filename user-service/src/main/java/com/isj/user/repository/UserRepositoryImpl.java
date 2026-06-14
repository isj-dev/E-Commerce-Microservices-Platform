package com.isj.user.repository;

import com.isj.user.domain.QUser;
import com.isj.user.domain.User;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    private final QUser user = QUser.user;

    @Override
    public Optional<User> findUserByEmail(String email) {
        return Optional.ofNullable(
                queryFactory.selectFrom(user)
                        .where(user.email.eq(email))
                        .fetchOne()
        );
    }

    @Override
    public boolean existsUserByEmail(String email) {
        return queryFactory
                .selectOne()
                .from(user)
                .where(user.email.eq(email))
                .fetchFirst() != null;
    }
}
