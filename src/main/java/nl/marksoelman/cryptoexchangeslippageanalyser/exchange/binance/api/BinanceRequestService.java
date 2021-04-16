package nl.marksoelman.cryptoexchangeslippageanalyser.exchange.binance.api;

import com.google.common.util.concurrent.RateLimiter;
import nl.marksoelman.cryptoexchangeslippageanalyser.RequestService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class BinanceRequestService extends RequestService {

    @Value("${binance.requestTimeoutSeconds}")
    private Integer requestTimeoutSeconds;

    private RateLimiter rateLimiter;

    public BinanceRequestService(@Value("${binance.requestsPerMinute}") Integer requestsPerMinute) {
        rateLimiter = RateLimiter.create(requestsPerMinute / 60.0);
    }

    @Override
    protected String getBaseURLWithoutEndingSlash() {
        return "https://api.binance.com";
    }

    @Override
    protected Integer getRequestTimeoutSeconds() {
        return requestTimeoutSeconds;
    }

    @Override
    protected RateLimiter getRateLimiter() {
        return rateLimiter;
    }

}
