package com.isj.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class OpenApiServerRewriteFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(OpenApiServerRewriteFilter.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.gateway-url:http://localhost:9000}")
    private String gatewayUrl;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        if (!path.startsWith("/service-docs/")) {
            return chain.filter(exchange);
        }

        ServerHttpResponse originalResponse = exchange.getResponse();
        ServerHttpResponseDecorator decorator = new ServerHttpResponseDecorator(originalResponse) {
            @Override
            public Mono<Void> writeWith(org.reactivestreams.Publisher<? extends DataBuffer> body) {
                return DataBufferUtils.join(Flux.from(body))
                        .flatMap(dataBuffer -> {
                            byte[] bytes = new byte[dataBuffer.readableByteCount()];
                            dataBuffer.read(bytes);
                            DataBufferUtils.release(dataBuffer);

                            try {
                                ObjectNode doc = (ObjectNode) objectMapper.readTree(bytes);

                                ArrayNode servers = objectMapper.createArrayNode();
                                ObjectNode server = objectMapper.createObjectNode();
                                server.put("url", gatewayUrl);
                                server.put("description", "API Gateway");
                                servers.add(server);
                                doc.set("servers", servers);

                                byte[] modified = objectMapper.writeValueAsBytes(doc);
                                getDelegate().getHeaders().set(HttpHeaders.CONTENT_LENGTH,
                                        String.valueOf(modified.length));
                                return super.writeWith(Mono.just(
                                        getDelegate().bufferFactory().wrap(modified)));
                            } catch (Exception e) {
                                log.error("Failed to rewrite OpenAPI servers field for path={}", path, e);
                                return super.writeWith(Mono.just(
                                        getDelegate().bufferFactory().wrap(bytes)));
                            }
                        });
            }
        };

        return chain.filter(exchange.mutate().response(decorator).build());
    }

    @Override
    public int getOrder() {
        return -2;
    }
}
