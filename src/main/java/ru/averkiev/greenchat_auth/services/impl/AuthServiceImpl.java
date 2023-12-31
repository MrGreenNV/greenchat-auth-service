package ru.averkiev.greenchat_auth.services.impl;

import io.jsonwebtoken.Claims;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import ru.averkiev.greenchat_auth.exceptions.AuthException;
import ru.averkiev.greenchat_auth.models.*;
import ru.averkiev.greenchat_auth.security.JwtAuthentication;
import ru.averkiev.greenchat_auth.security.JwtProvider;
import ru.averkiev.greenchat_auth.services.AccessTokenService;
import ru.averkiev.greenchat_auth.services.AuthService;
import ru.averkiev.greenchat_auth.services.RefreshTokenService;

/**
 * Класс предоставляет функционал для аутентификации и авторизации пользователей.
 * @author mrGreenNV
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserDetailsService jwtUserDetailsService;
    private final AccessTokenService accessTokenService;
    private final RefreshTokenService refreshTokenService;
    private final JwtProvider jwtProvider;
    private final BCryptPasswordEncoder passwordEncoder;

    /**
     * Выполняет аутентификацию пользователя при входе в систему.
     * @param authRequest - запрос на аутентификацию пользователя.
     * @return - ответ на запрос аутентификации пользователя, содержащий access и refresh токены.
     * @throws AuthException - выбрасывается, если был передан невалидный пароль.
     */
    @Override
    public JwtResponse login(JwtRequest authRequest) throws AuthException {

        // Получение данных из микросервиса пользователей.
        final JwtUser jwtUser = (JwtUser) jwtUserDetailsService.loadUserByUsername(authRequest.getLogin());

        // Сравнение пароля, полученного из запроса аутентификации с паролем, полученным от микросервиса
        // пользователей.
        if (passwordEncoder.matches(authRequest.getPassword(), jwtUser.getPassword())) {
            // Генерация access токена с помощью JwtProvider.
            final String accessTokenStr = jwtProvider.generateAccessToken(jwtUser);
            // Создание объекта AccessToken.
            AccessToken accessToken = new AccessToken(
                    jwtUser.getId(),
                    accessTokenStr,
                    jwtProvider.getAccessClaims(accessTokenStr).getIssuedAt(),
                    jwtProvider.getAccessClaims(accessTokenStr).getExpiration()
            );

            // Сохранение access токена в базе данных.
            if (accessTokenService.findByUserId(jwtUser.getId()).isPresent()) {
                accessTokenService.update(jwtUser.getId(), accessToken);
            } else {
                accessTokenService.save(accessToken);
            }

            // Генерация access токена с помощью JwtProvider.
            final String refreshTokenStr = jwtProvider.generateRefreshToken(jwtUser);
            // Создание объекта AccessToken.
            RefreshToken refreshToken = new RefreshToken(
                    jwtUser.getId(),
                    refreshTokenStr,
                    jwtProvider.getRefreshClaims(refreshTokenStr).getIssuedAt(),
                    jwtProvider.getRefreshClaims(refreshTokenStr).getExpiration()
            );

            // Сохранение access токена в базе данных.
            if (refreshTokenService.findByUserId(jwtUser.getId()).isPresent()) {
                refreshTokenService.update(jwtUser.getId(), refreshToken);
            } else {
                refreshTokenService.save(refreshToken);
            }

            return new JwtResponse(accessTokenStr, refreshTokenStr);
        } else {
            throw new AuthException("Неправильный пароль");
        }
    }

    /**
     * Получение нового access токена на основе переданного refresh токена.
     * @param refreshToken - refresh токен.
     * @return объект JwtResponse, содержащий новый access токен.
     */
    @Override
    public JwtResponse getAccessToken(String refreshToken) {
        if (jwtProvider.validateRefreshToken(refreshToken)) {
            final Claims claims = jwtProvider.getRefreshClaims(refreshToken);
            final String username = claims.getSubject();

            final JwtUser jwtUser = (JwtUser) jwtUserDetailsService.loadUserByUsername(username);
            final RefreshToken saveRefreshToken = refreshTokenService.findByUserId(jwtUser.getId()).orElse(null);

            if (saveRefreshToken != null && saveRefreshToken.getRefreshToken().equals(refreshToken)) {
                // Генерация access токена с помощью JwtProvider.
                final String accessTokenStr = jwtProvider.generateAccessToken(jwtUser);
                // Создание объекта AccessToken.
                final AccessToken newAccessToken = new AccessToken(
                        jwtUser.getId(),
                        accessTokenStr,
                        jwtProvider.getAccessClaims(accessTokenStr).getIssuedAt(),
                        jwtProvider.getAccessClaims(accessTokenStr).getExpiration()
                );

                // Обновление access токена в базе данных.
                accessTokenService.update(jwtUser.getId(), newAccessToken);
                return new JwtResponse(accessTokenStr, null);
            }
        }
        return new JwtResponse(null, null);
    }

    /**
     * Обновление access и refresh токенов, на основе переданного refresh токена.
     * @param refreshToken - refresh токен.
     * @return - объект JwtResponse, содержащий новые access и refresh токены.
     * @throws AuthException выбрасывается, если передан недействительный JWT токен.
     */
    @Override
    public JwtResponse refresh(String refreshToken) {
        if (jwtProvider.validateRefreshToken(refreshToken)) {
            final Claims claims = jwtProvider.getRefreshClaims(refreshToken);
            final String username = claims.getSubject();

            final JwtUser jwtUser = (JwtUser) jwtUserDetailsService.loadUserByUsername(username);
            final RefreshToken saveRefreshToken = refreshTokenService.findByUserId(jwtUser.getId()).orElse(null);

            if (saveRefreshToken != null && saveRefreshToken.getRefreshToken().equals(refreshToken)) {

                // Генерация access токена с помощью JwtProvider.
                final String accessTokenStr = jwtProvider.generateAccessToken(jwtUser);
                // Создание объекта AccessToken.
                final AccessToken newAccessToken = new AccessToken(
                        jwtUser.getId(),
                        accessTokenStr,
                        jwtProvider.getAccessClaims(accessTokenStr).getIssuedAt(),
                        jwtProvider.getAccessClaims(accessTokenStr).getExpiration()
                );

                // Генерация refresh токена с помощью JwtProvider.
                final String refreshTokenStr = jwtProvider.generateRefreshToken(jwtUser);
                // Создание объекта RefreshToken.
                final RefreshToken newRefreshToken = new RefreshToken(
                        jwtUser.getId(),
                        refreshTokenStr,
                        jwtProvider.getRefreshClaims(refreshTokenStr).getIssuedAt(),
                        jwtProvider.getRefreshClaims(refreshTokenStr).getExpiration()
                );

                // Обновление access токена в базе данных.
                accessTokenService.update(jwtUser.getId(), newAccessToken);
                // Обновление refresh токена в базе данных.
                refreshTokenService.update(jwtUser.getId(), newRefreshToken);

                return new JwtResponse(newAccessToken.getAccessToken(), newRefreshToken.getRefreshToken());
            }
        }
        throw new AuthException("Неверный JWT токен");
    }

    /**
     * Получение информации об аутентификации пользователя.
     * @return JwtAuthentication, содержащий информацию об аутентификации пользователя.
     */
    @Override
    public JwtAuthentication getAuthInfo() {
        return (JwtAuthentication) SecurityContextHolder.getContext().getAuthentication();
    }

    /**
     * Осуществление пользователем выхода из системы, путём удаления токенов.
     * @param refreshToken refresh токен.
     * @return true, если выход успешно осуществлён, иначе false.
     */
    @Override
    @Transactional
    public boolean logout(String refreshToken) {
        if (jwtProvider.validateRefreshToken(refreshToken)) {
            final Claims claims = jwtProvider.getRefreshClaims(refreshToken);
            final String username = claims.getSubject();

            final JwtUser jwtUser = (JwtUser) jwtUserDetailsService.loadUserByUsername(username);
            refreshTokenService.delete(jwtUser.getId());
            accessTokenService.delete(jwtUser.getId());

            return true;
        }
        return false;
    }

    /**
     * Проверка валидности refresh токена.
     * @param refreshToken проверяемый refresh токен.
     * @return результат проверки, true если успешно, иначе false.
     */
    @Override
    public boolean validate(String refreshToken) {
        return jwtProvider.validateRefreshToken(refreshToken);
    }
}