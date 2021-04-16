package nl.marksoelman.cryptoexchangeslippageanalyser;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.util.concurrent.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import nl.marksoelman.cryptoexchangeslippageanalyser.exchange.binance.model.BinanceCandle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
public abstract class RequestService {

    abstract protected String getBaseURLWithoutEndingSlash();
    abstract protected Integer getRequestTimeoutSeconds();
    abstract protected RateLimiter getRateLimiter();

    @Autowired
    WebClient webClient;

    public <T> Mono<T> getSingle(String path, Class<T> returnType) {
        return (Mono<T>) this.makeRequest(HttpMethod.GET, getBaseURLWithoutEndingSlash() + path,
                returnType, true);
    }

    public <T> Mono<List<T>> getMany(String path, Class<T> returnType) {
        return (Mono<List<T>>) this.makeRequest(HttpMethod.GET, getBaseURLWithoutEndingSlash() + path,
                returnType, false);
    }

    protected <T> Mono<?> makeRequest(HttpMethod method, String URL, Class<T> returnType,
                                      boolean isSingleItemResponse) {
        // Rate limiting, if any
        this.getRateLimiter().acquire();

//        log.info("Making " + method.name() + "-request to: " + URL);

        // Make request
        WebClient.ResponseSpec request = webClient.method(method)
                .uri(URL)
                .retrieve();

        // Parse request
        Mono<?> response;
        if(isSingleItemResponse) {
            response = request.bodyToMono(returnType)
                    .retryWhen(
                            Retry.fixedDelay(Long.MAX_VALUE, Duration.ofSeconds(getRequestTimeoutSeconds()))
                                    .filter(this::shouldRetry)
                    )
                    .cache();
        } else {
            response = request.bodyToMono(returnType.arrayType())
                    .retryWhen(
                            Retry.fixedDelay(Long.MAX_VALUE, Duration.ofSeconds(getRequestTimeoutSeconds()))
                                    .filter(this::shouldRetry)
                    )
                    .map(arrayOfT -> Arrays.asList((T[]) arrayOfT))
                    .cache();
        }

        return response;
    }

    private boolean shouldRetry(Throwable throwable) {
        boolean shouldRetry = false;
        String message = throwable.getMessage();

        if(throwable instanceof WebClientResponseException) {
            int errorCode = ((WebClientResponseException) throwable).getRawStatusCode();
            message = ((WebClientResponseException) throwable).getResponseBodyAsString();

            // Too many requests
            shouldRetry = errorCode == 429 // too many requests
                || errorCode == 418; // Banned "I am a teapot"
        }

        log.warn("Request failed due to: " + message + ". " +
                ((shouldRetry) ? "Retrying after sleep." : "Not retrying request."));

        return shouldRetry;
    }
}
