package nl.marksoelman.cryptoexchangeslippageanalyser.exchange.binance.api;

import nl.marksoelman.cryptoexchangeslippageanalyser.exchange.binance.api.BinanceRequestService;
import nl.marksoelman.cryptoexchangeslippageanalyser.exchange.binance.model.BinanceOrderBookAsk;
import nl.marksoelman.cryptoexchangeslippageanalyser.exchange.binance.model.BinanceOrderBookBid;
import nl.marksoelman.cryptoexchangeslippageanalyser.exchange.binance.model.OrderBookDepth;
import nl.marksoelman.cryptoexchangeslippageanalyser.model.Instrument;
import nl.marksoelman.cryptoexchangeslippageanalyser.model.OrderBook;
import nl.marksoelman.cryptoexchangeslippageanalyser.model.OrderBookAsk;
import nl.marksoelman.cryptoexchangeslippageanalyser.model.OrderBookBid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderbookFetcher {

    @Autowired
    BinanceRequestService binanceRequestService;

    @Value("${binance.orderbook-depth-to-fetch}")
    private Integer orderBookDepth;

    public Mono<OrderBook> getOrderBook(Instrument instrument) {
        return binanceRequestService.getSingle(
                "/api/v3/depth?symbol=" + instrument.getSymbolConcat() + "&limit=" + orderBookDepth, OrderBookDepth.class)
                .map(orderBookDepth -> {
                    List<OrderBookBid> bids = orderBookDepth.getBids().stream()
                            .map(bbid -> new OrderBookBid(
                                    Double.parseDouble(bbid.getPrice()),
                                    Double.parseDouble(bbid.getQuantity()))
                            ).collect(Collectors.toList());
                    List<OrderBookAsk> asks = orderBookDepth.getAsks().stream()
                            .map(basks -> new OrderBookAsk(
                                    Double.parseDouble(basks.getPrice()),
                                    Double.parseDouble(basks.getQuantity()))
                            ).collect(Collectors.toList());

                    return new OrderBook(instrument, bids, asks);
                });
    }
}
