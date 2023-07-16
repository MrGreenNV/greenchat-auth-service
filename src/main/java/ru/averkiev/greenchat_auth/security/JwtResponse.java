package ru.averkiev.greenchat_auth.security;

import lombok.Data;

/**
 * Класс представляет ответ на запрос аутентификации с использованием JWT.
 * Этот класс используется для передачи информации о токенах доступа и обновления от микросервиса аутентификации
 * и авторизации клиенту в ответ на успешную аутентификацию.
 * Содержит в себе информацию об access и refresh токенах.
 * @author mrGreenNV
 */
@Data
public class JwtResponse {
    private final String type = "Bearer";
    private String accessToken;
    private String refreshToken;
}