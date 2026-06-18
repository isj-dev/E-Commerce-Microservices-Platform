package com.isj.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

// 클라이언트가 API를 호출할 때 토큰이 유효한지 Gateway에서 먼저 검증하고, 유효하면 사용자 정보를 헤더에 담아서 각 서비스로 전달합니다
@Component
public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {

    @Value("${jwt.secret}")
    private String jwtSecret;

    public JwtAuthenticationFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> { // // exchange=요청/응답, chain=다음 필터 체인
            String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return onError(exchange, HttpStatus.UNAUTHORIZED);
            }

            String token = authHeader.substring(7);  // "Bearer " 이후 토큰만 추출

            try {
                Claims claims = parseClaims(token); // 토큰 파싱 및 서명 검증
                ServerWebExchange mutatedExchange = exchange.mutate()
                        .request(r -> r.header("X-User-Id", claims.getSubject()) // userId 추가
                                .header("X-User-Email", claims.get("email", String.class))) // email 추가
                        .build();
                return chain.filter(mutatedExchange); // 헤더가 추가된 요청을 다음으로 전달
            } catch (Exception e) {
                return onError(exchange, HttpStatus.UNAUTHORIZED);
            }
        };
    }

    private Claims parseClaims(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(key) // 서명 검증에 사용할 키 설정
                .build()
                .parseSignedClaims(token) // 서명 검증 및 파싱
                .getPayload(); // Claims(토큰 내용) 반환
    }

    // Mono<Void>를 반환하는 이유는 Spring Cloud Gateway가 WebFlux(비동기/리액티브) 기반이기 때문
    // Mono는 리액티브 프로그래밍에서 0개 또는 1개의 결과를 나타내는 타입
    private Mono<Void> onError(ServerWebExchange exchange, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        return exchange.getResponse().setComplete();
    }

    public static class Config {
    }
}
