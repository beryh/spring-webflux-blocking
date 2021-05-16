package com.jdriven.mib.webflux.webfluxblocking;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController()
@RequestMapping("block")
public class BlockingEndpoint {
    private final static Logger LOGGER = LoggerFactory.getLogger(BlockingEndpoint.class);

    @GetMapping(value = "time/{sleepMs}", produces = MediaType.TEXT_PLAIN_VALUE)
    public Mono<String> block(@PathVariable long sleepMs) {
        return Mono.fromSupplier(() -> blockingFunction(sleepMs))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private String blockingFunction(long sleepMs) {
        LOGGER.info("Start sleep for " + sleepMs);
        try {
            Thread.sleep(sleepMs);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        LOGGER.info("End sleep for " + sleepMs);
        return "OK, wake up from " + sleepMs + " ms sleep";
    }

    @GetMapping(value = "health", produces = MediaType.TEXT_PLAIN_VALUE)
    public String health() {
        return "OK";
    }
}
