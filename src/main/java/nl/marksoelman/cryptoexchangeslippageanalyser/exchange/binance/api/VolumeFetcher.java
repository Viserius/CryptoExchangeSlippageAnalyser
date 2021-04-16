package nl.marksoelman.cryptoexchangeslippageanalyser.exchange.binance.api;

import lombok.extern.slf4j.Slf4j;
import nl.marksoelman.cryptoexchangeslippageanalyser.exchange.binance.model.BinanceCandle;
import nl.marksoelman.cryptoexchangeslippageanalyser.model.Instrument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@Service @Slf4j
public class VolumeFetcher {

    @Autowired
    BinanceRequestService binanceRequestService;

    public double fetch(Instrument instrument, int numberOfDays) {
        String path = "/api/v3/klines?symbol=" + instrument.getSymbolConcat();
        path += "&interval=1d";
        path += "&startTime=" + LocalDate.now().minusDays(numberOfDays-1).atStartOfDay().toEpochSecond(ZoneOffset.UTC) * 1000;

        Mono<List<BinanceCandle>> candlesMono = binanceRequestService.getMany(path, BinanceCandle.class);
        List<BinanceCandle> candles = candlesMono.block();

        return candles.stream().mapToDouble(BinanceCandle::getQuoteAssetVolume).sum();
    }
}
