package com.jdriven.mib.webflux.webfluxblocking;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.blockhound.BlockHound;

import java.time.Duration;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Execution(ExecutionMode.CONCURRENT)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class BlockingEndpointIT {
    @BeforeAll
    public static void beforeAll() {
        BlockHound.install();
        System.setProperty("reactor.netty.ioWorkerCount", "4"); // set to minimum 4, I got 16 CPU cores
    }

    private final WebTestClient webClient = WebTestClient.bindToServer()
        .baseUrl("http://localhost:8080")
        .responseTimeout(Duration.ofMillis(30000))
        .build();

    @Test
    public void testBlocking() {
        this.webClient.get().uri("/block/time/11000")
            .exchange()
            .expectStatus().isOk();
    }

    @ParameterizedTest
    @MethodSource("numberOfTests")
    void health(int nr) {
        this.webClient.get().uri("/block/health")
            .exchange()
            .expectStatus().isOk();
    }

    private static Stream<Arguments> numberOfTests() {
        return IntStream.range(1, 10).mapToObj(Arguments::arguments);
    }
}
