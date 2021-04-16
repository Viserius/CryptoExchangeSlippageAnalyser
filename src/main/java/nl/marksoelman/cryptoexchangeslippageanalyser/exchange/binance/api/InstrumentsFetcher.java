package nl.marksoelman.cryptoexchangeslippageanalyser.exchange.binance.api;

import nl.marksoelman.cryptoexchangeslippageanalyser.exchange.binance.model.ExchangeInfoResponse;
import nl.marksoelman.cryptoexchangeslippageanalyser.model.Instrument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Set;

@Service
public class InstrumentsFetcher {

    @Autowired
    BinanceRequestService binanceRequestService;

    public Flux<Instrument> getInstruments() {
        Mono<ExchangeInfoResponse> exchangeInfo = binanceRequestService.getSingle(
                "/api/v3/exchangeInfo", ExchangeInfoResponse.class);

        return exchangeInfo.flatMapIterable(ExchangeInfoResponse::getSymbols)
                .map(sr -> new Instrument(sr.getBaseAsset(), sr.getQuoteAsset()));
    }

}
