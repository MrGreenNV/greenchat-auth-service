package ru.averkiev.greenchat_auth.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.averkiev.greenchat_auth.models.AccessToken;

import java.util.Optional;

/**
 * Интерфейс представляет собой репозиторий access токенов.
 */
@Repository
public interface AccessTokenRepository extends JpaRepository<AccessToken, Integer> {
    Optional<AccessToken> findByUserId(int UserId);

    void deleteByUserId(int userId);
}
