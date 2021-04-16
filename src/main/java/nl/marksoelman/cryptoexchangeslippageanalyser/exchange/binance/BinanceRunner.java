package nl.marksoelman.cryptoexchangeslippageanalyser.exchange.binance;

import nl.marksoelman.cryptoexchangeslippageanalyser.ExchangeRunner;
import nl.marksoelman.cryptoexchangeslippageanalyser.exchange.binance.api.InstrumentsFetcher;
import nl.marksoelman.cryptoexchangeslippageanalyser.exchange.binance.api.OrderbookFetcher;
import nl.marksoelman.cryptoexchangeslippageanalyser.exchange.binance.api.VolumeFetcher;
import nl.marksoelman.cryptoexchangeslippageanalyser.model.Instrument;
import nl.marksoelman.cryptoexchangeslippageanalyser.model.OrderBook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BinanceRunner implements ExchangeRunner {

    @Autowired
    InstrumentsFetcher instrumentsFetcher;

    @Autowired
    OrderbookFetcher orderbookFetcher;

    @Autowired
    VolumeFetcher volumeFetcher;

    @Override
    public List<Instrument> getInstruments() {
        return instrumentsFetcher.getInstruments().collectList().block();
    }

    @Override
    public OrderBook getOrderBook(Instrument instrument) {
        return orderbookFetcher.getOrderBook(instrument).block();
    }

    @Override
    public double getQuoteVolume(Instrument instrument, int numberOfDays) {
        return this.volumeFetcher.fetch(instrument, 7);
    }

}
