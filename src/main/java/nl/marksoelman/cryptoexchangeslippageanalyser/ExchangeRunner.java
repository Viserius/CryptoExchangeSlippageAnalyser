package nl.marksoelman.cryptoexchangeslippageanalyser;

import nl.marksoelman.cryptoexchangeslippageanalyser.model.Instrument;
import nl.marksoelman.cryptoexchangeslippageanalyser.model.OrderBook;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Set;

public interface ExchangeRunner {

    List<Instrument> getInstruments();

    OrderBook getOrderBook(Instrument instrument);

    double getQuoteVolume(Instrument instrument, int numberOfDays);

}
