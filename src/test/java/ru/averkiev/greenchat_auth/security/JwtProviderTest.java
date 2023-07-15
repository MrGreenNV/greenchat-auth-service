package ru.averkiev.greenchat_auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import ru.averkiev.greenchat_auth.models.User;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Тестовый класс для проверки функциональности класса JwtProvider. Этот класс предоставляет функциональность для
 * создания, проверки и валидации JWT токенов.
 */
public class JwtProviderTest {

    private static final String JWT_ACCESS_SECRET =
            "and0QWNjZXNzVG9rZW5qd3RBY2Nlc3NUb2tlbmp3dEFjY2Vzc1Rva2Vuand0QWNjZXNzVG9rZW5qd3RBY2Nlc3NUb2tlbg==";
    private static final String JWT_REFRESH_SECRET =
            "and0UmVmcmVzaFRva2Vuand0UmVmcmVzaFRva2Vuand0UmVmcmVzaFRva2Vuand0UmVmcmVzaFRva2Vuand0UmVmcmVzaFRva2Vu";

    private JwtProvider jwtProvider;

    // Создание тестовых данных.
    JwtUser jwtUser = JwtUserFactory.created(new User(
            0,
            "Bob_Smith",
            "pass132456",
            "Bob",
            "Smith",
            "bob@gmail.com",
            "ACTIVE",
            Set.of("user", "admin")
    ));

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        jwtProvider = new JwtProvider(JWT_ACCESS_SECRET, JWT_REFRESH_SECRET);
    }

    /**
     * Проверяет генерацию access токена, а затем извлекает из него payload и сравнивает с ожидаемым результатом.
     */
    @Test
    public void testGenerateAccessToken() {
        // Создание тестовых данных.
        LocalDateTime now = LocalDateTime.now();
        Date expiration = Date.from(now.plusMinutes(5).atZone(ZoneId.systemDefault()).toInstant());

        //Генерация токена доступа.
        String accessToken = jwtProvider.generateAccessToken(jwtUser);

        // Проверка результатов.
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(Decoders.BASE64.decode(JWT_ACCESS_SECRET)))
                .build()
                .parseClaimsJws(accessToken)
                .getBody();

        List<Map<String, String>> authorities = claims.get("authorities", List.class);
        Set<SimpleGrantedAuthority> actualRoles = authorities.stream()
                .map(map -> new SimpleGrantedAuthority(map.get("authority")))
                .collect(Collectors.toSet());

        Assertions.assertEquals(jwtUser.getUsername(), claims.getSubject());
        Assertions.assertEquals(jwtUser.getFirstname(), claims.get("firstname"));
        Assertions.assertEquals(jwtUser.getLastname(), claims.get("lastname"));
        Assertions.assertEquals(expiration.toString(), claims.getExpiration().toString());
        Assertions.assertEquals(jwtUser.getAuthorities(), actualRoles);
    }

    /**
     * Проверяет генерацию refresh токена, а затем извлекает из него payload и сравнивает с ожидаемым результатом.
     */
    @Test
    public void testGenerateRefreshToken() {
        // Создание тестовых данных.
        String username = "Bob_Smith";

        LocalDateTime now = LocalDateTime.now();
        Date expiration = Date.from(now.plusDays(7).atZone(ZoneId.systemDefault()).toInstant());

        // Генерация refresh токена.
        String refreshToken = jwtProvider.generateRefreshToken(jwtUser);

        // Проверка результатов.
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(Decoders.BASE64.decode(JWT_REFRESH_SECRET)))
                .build()
                .parseClaimsJws(refreshToken)
                .getBody();

        Assertions.assertEquals(username, claims.getSubject());
        Assertions.assertEquals(expiration.toString(), claims.getExpiration().toString());
    }

    /**
     * Проверяет валидность действующего access токена.
     */
    @Test
    public void testValidateAccessToken_ValidToken() {
        // Генерация валидного токена.
        String accessToken = jwtProvider.generateAccessToken(jwtUser);

        // Проверка валидности access токена.
        Assertions.assertTrue(jwtProvider.validateAccessToken(accessToken));
    }

    /**
     * Проверяет валидность неправильного access токена.
     */
    @Test
    public void testValidateAccessToken_ExpiredToken() {
        // Генерация не валидного access токена.
        final LocalDateTime now = LocalDateTime.now();
        final Instant accessExpirationInstance = now.plusMinutes(0).atZone(ZoneId.systemDefault()).toInstant();
        final Date accessExpiration = Date.from(accessExpirationInstance);
        String accessToken = Jwts.builder()
                .setSubject(jwtUser.getUsername())
                .setExpiration(accessExpiration)
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(JWT_ACCESS_SECRET)))
                .claim("firstname", jwtUser.getFirstname())
                .claim("lastname", jwtUser.getLastname())
                .claim("authorities", jwtUser.getAuthorities())
                .compact();

        // Проверка валидности access токена.
        Assertions.assertFalse(jwtProvider.validateAccessToken(accessToken));
    }

    /**
     * Проверяет валидность действующего refresh токена.
     */
    @Test
    public void testGenerateRefreshToken_ValidToken() {
        // Генерация валидного токена.
        String refreshToken = jwtProvider.generateRefreshToken(jwtUser);

        // Проверка валидности refresh токена.
        Assertions.assertTrue(jwtProvider.validateRefreshToken(refreshToken));
    }

    /**
     * Проверяет валидность неправильного refresh токена.
     */
    @Test
    public void testGenerateRefreshToken_ExpiredToken() {
        // Генерация не валидного refresh токена.
        final LocalDateTime now = LocalDateTime.now();
        final Instant refreshExpirationInstant = now.plusDays(0).atZone(ZoneId.systemDefault()).toInstant();
        final Date refreshExpiration = Date.from(refreshExpirationInstant);
        String refreshToken = Jwts.builder()
                .setSubject(jwtUser.getUsername())
                .setExpiration(refreshExpiration)
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(JWT_REFRESH_SECRET)))
                .compact();

        // Проверка валидности refresh токена.
        Assertions.assertFalse(jwtProvider.validateRefreshToken(refreshToken));
    }
}