package nl.marksoelman.cryptoexchangeslippageanalyser.exchange.binance.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Data @NoArgsConstructor @AllArgsConstructor @Slf4j
public class BinanceCandle {

    private LocalDateTime openTime;
    private double openPrice;
    private double highPrice;
    private double lowPrice;
    private double closePrice;
    private double baseAssetVolume;
    private LocalDateTime closeTime;
    private double quoteAssetVolume;
    private long numberOfTrades;
    private double takerBuyBaseAssetVolume;
    private double takerBuyQuoteAssetVolume;

    @JsonCreator
    public BinanceCandle(Object[] candle) {
        this.openTime = LocalDateTime.ofEpochSecond((long) candle[0] / 1000,
                (int) ((long) candle[0] % 1000), ZoneOffset.UTC);
        this.openPrice = Double.parseDouble((String) candle[1]);
        this.highPrice = Double.parseDouble((String) candle[2]);
        this.lowPrice = Double.parseDouble((String) candle[3]);
        this.closePrice = Double.parseDouble((String) candle[4]);
        this.baseAssetVolume = Double.parseDouble((String) candle[5]);
        this.closeTime = LocalDateTime.ofEpochSecond((long) candle[6] / 1000,
                (int) ((long) candle[6] % 1000), ZoneOffset.UTC);
        this.quoteAssetVolume = Double.parseDouble((String) candle[7]);
        this.numberOfTrades = (int) candle[8];
        this.takerBuyBaseAssetVolume = Double.parseDouble((String) candle[9]);
        this.takerBuyQuoteAssetVolume = Double.parseDouble((String) candle[10]);
    }

}
