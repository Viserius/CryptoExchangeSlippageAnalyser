package nl.marksoelman.cryptoexchangeslippageanalyser;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import nl.marksoelman.cryptoexchangeslippageanalyser.exchange.binance.BinanceRunner;
import nl.marksoelman.cryptoexchangeslippageanalyser.exchange.binance.api.VolumeFetcher;
import nl.marksoelman.cryptoexchangeslippageanalyser.model.Instrument;
import nl.marksoelman.cryptoexchangeslippageanalyser.model.OrderBook;
import nl.marksoelman.cryptoexchangeslippageanalyser.model.OrderBookAsk;
import nl.marksoelman.cryptoexchangeslippageanalyser.model.OrderBookBid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component @Slf4j
public class SlippageRunner {

    @Value("${core.usdTetheredCoin}")
    private String usdTetheredCoin;

    @Value("${core.timeoutBetweenUpdateSeconds}")
    private Integer timeoutBetweenUpdateSeconds;

    @Value("#{'${core.quoteWhitelistToAnalyze}'.split(',')}")
    private List<String> whitelistedQuotes;

    @Value("${core.fees.included}")
    private Boolean feesIncluded;

    @Value("${core.fees.feePct}")
    private Double feePct;

    @Value("${core.orderByVolume.enabled}")
    private Boolean orderByVolume;

    @Value("${core.orderByVolume.days}")
    private int volumeDays;

    @Autowired
    private CSVExporter csvExporter;

    ExchangeRunner exchangeRunner;

    public SlippageRunner(@Value("${core.exchange}") String exchange,
                          @Autowired BinanceRunner binanceRunner
    ) {
        if(exchange.equalsIgnoreCase("binance"))
            exchangeRunner = binanceRunner;
        else
            throw new IllegalArgumentException("Argument: " + exchange + " unsupported.");
    }

    @SneakyThrows
    public void run(double orderSizeUSD) {
        log.info("Fetching all tradable instruments of Exchange.");
        List<Instrument> instrumentList = exchangeRunner.getInstruments().stream()
                .filter(i -> whitelistedQuotes.size() == 0 || whitelistedQuotes.contains(i.getQuoteCurrency()))
                .collect(Collectors.toList());
        log.info("A total of " + instrumentList.size() + " instruments have been fetched from exchange.");

        HashMap<Instrument, List<Double>> instrumentSlippagePct = new HashMap<>();
        Map<Instrument, Double> instrumentUsdVolume = null;
        int i = 1;
        while(true) {
            LocalDateTime start = LocalDateTime.now();

            log.info("For iteration #" + i + ", fetch order books for each and every instrument.");
            Map<Instrument, OrderBook> instrumentOrderBookMap = instrumentList.parallelStream()
                    .map(instrument -> exchangeRunner.getOrderBook(instrument))
                    .filter(OrderBook::nonEmpty)
                    .collect(Collectors.toMap(OrderBook::getInstrument, ob -> ob));
            log.info("A total of " + instrumentOrderBookMap.size() + " order books were fetched.");

            log.info("Generating map of Quote->USDValue.");
            Map<String, Double> quoteToUsdPrice = orderBooksToUsd(instrumentOrderBookMap);
            log.info("USD values were derived for a total of " + quoteToUsdPrice.size() + " quotes.");

            log.info("Computing BUY and SELL slippage for each instrument.");
            instrumentOrderBookMap.values()
                    .forEach(ob -> {
                        if(!instrumentSlippagePct.containsKey(ob.getInstrument()))
                            instrumentSlippagePct.put(ob.getInstrument(), new ArrayList<>());

                        instrumentSlippagePct.get(ob.getInstrument()).add(
                                this.computeSlippageBuy(
                                        ob,
                                        quoteToUsdPrice.get(ob.getInstrument().getQuoteCurrency()),
                                        orderSizeUSD
                                )
                        );

                        instrumentSlippagePct.get(ob.getInstrument()).add(
                                this.computeSlippageSell(
                                        ob,
                                        quoteToUsdPrice.get(ob.getInstrument().getQuoteCurrency()),
                                        orderSizeUSD
                                )
                        );
                    });

            if(orderByVolume) {
                if(instrumentUsdVolume == null) {
                    log.info("Fetching Volume for each pair");
                    instrumentUsdVolume = computeVolumes(new ArrayList<>(instrumentSlippagePct.keySet()),
                            volumeDays, quoteToUsdPrice);
                }

                log.info("Storing (intermediate) slippage results, ordered by USD Volume, in CSV.");
                csvExporter.export(instrumentSlippagePct, Optional.of(
                        instrumentUsdVolume.entrySet()
                                .stream()
                                .sorted((a, b) -> Double.compare(b.getValue(),
                                a.getValue()))
                                .map(Map.Entry::getKey)
                ));
            } else {
                log.info("Storing (intermediate) slippage results in CSV.");
                csvExporter.export(instrumentSlippagePct, Optional.empty());
            }

            // Sleep until next update
            long totalTimeForIteration = ChronoUnit.SECONDS.between(start, LocalDateTime.now());
            log.info("Iteration #" + i + " finished in " + totalTimeForIteration + " seconds.");
            long timeout = timeoutBetweenUpdateSeconds - totalTimeForIteration;
            if(timeout > 0) {
                log.info("Now sleeping for " + timeout + " seconds until next iteration.");
                Thread.sleep(timeout * 1000);
            }

            i++;
        }
    }

    private double computeSlippageBuy(OrderBook orderBook, double quoteUsdValue, double totalUsdToSpend) {
        double quoteToSpend = totalUsdToSpend / quoteUsdValue;
        double totalAssetsBought = 0;
        for(int i = 0; i < orderBook.getAsks().size(); i++) {
            if(quoteToSpend <= 0)
                break;

            OrderBookAsk ask = orderBook.getAsks().get(i);
            double availableQuantity = ask.getQuantity();
            double availablePrice = ask.getPrice();
            double amountToPurchase = Math.min(availableQuantity, quoteToSpend / availablePrice);

            totalAssetsBought += afterFee(amountToPurchase);
            quoteToSpend -= amountToPurchase * availablePrice;
        }

        double expectedBought = totalUsdToSpend / quoteUsdValue / orderBook.getMidpointPrice();
        double slippage = (1 - totalAssetsBought / expectedBought);
//        log.error("On instrument: " + orderBook.getInstrument() + ", slippage BUY=" +
//                String.format("%.2f", slippage*100) + "%.");

        return slippage;
    }

    private double computeSlippageSell(OrderBook orderBook, double quoteUsdValue, double totalUsdToSpend) {
        double quoteToObtainExpected = totalUsdToSpend / quoteUsdValue;
        double baseToSell = quoteToObtainExpected / orderBook.getMidpointPrice();
        double quoteObtained = 0d;

        for(int i = 0; i < orderBook.getBids().size(); i++) {
            if(baseToSell <= 0)
                break;

            OrderBookBid bid = orderBook.getBids().get(i);
            double availableQuantity = bid.getQuantity();
            double availablePrice = bid.getPrice();
            double amountToSell = Math.min(availableQuantity, baseToSell);

            quoteObtained += afterFee(amountToSell * availablePrice);
            baseToSell -= amountToSell;
        }

        double slippage = (1 - quoteObtained / quoteToObtainExpected);
//        log.error("On instrument: " + orderBook.getInstrument() + ", slippage SELL=" +
//                String.format("%.2f", slippage*100) + "%.");

        return slippage;
    }

    private HashMap<Instrument, List<Double>> initializeSlippageList(List<Instrument> instrumentList) {
        HashMap<Instrument, List<Double>> list = new HashMap<>();
        instrumentList
                .forEach(i -> list.put(i, new ArrayList<>()));
        return list;
    }

    private Map<String, Double> orderBooksToUsd(Map<Instrument, OrderBook> instrumentOrderBookMap) {

        // Heap of currencies to map to Usd value
        Set<String> instrumentsToGetUsd = instrumentOrderBookMap.keySet().stream()
                .flatMap(i -> Stream.of(i.getQuoteCurrency(), i.getBaseCurrency()))
                .collect(Collectors.toSet());

        // try to find the usd values, but not infinitely long
        Map<String, Double> instrumentUsdValue = new HashMap<>();
        instrumentUsdValue.put(usdTetheredCoin, 1.0);
        instrumentsToGetUsd.remove(usdTetheredCoin);

        for(int attempt = 0; attempt < 5; attempt++) {
            for (Map.Entry<Instrument, OrderBook> entryToCheck : instrumentOrderBookMap.entrySet()) {
                Instrument instrumentToCheck = entryToCheck.getKey();
                String base = instrumentToCheck.getBaseCurrency();
                String quote = instrumentToCheck.getQuoteCurrency();
                OrderBook orderBook = entryToCheck.getValue();

                // Base currency's value in USD is required
                if (instrumentsToGetUsd.contains(base) && instrumentUsdValue.containsKey(quote)) {
                    double priceOfBaseQuote = orderBook.getMidpointPrice();
                    double priceOfBaseUsd = priceOfBaseQuote * instrumentUsdValue.get(quote);
                    instrumentUsdValue.put(base, priceOfBaseUsd);
                    instrumentsToGetUsd.remove(base);
//                    log.error("USD price of " + base + " is estimated at " + priceOfBaseUsd + " because " + base + "/" +
//                            quote + "=" + priceOfBaseQuote + " and " + quote + "=" + instrumentUsdValue.get(quote));
                    continue;
                }

                // Quote currency's value in USD is required
                if (instrumentsToGetUsd.contains(quote) && instrumentUsdValue.containsKey(base)) {
                    double priceOfBaseQuote = orderBook.getMidpointPrice();
                    double priceOfQuoteUsd = instrumentUsdValue.get(base) / priceOfBaseQuote;
                    instrumentUsdValue.put(quote, priceOfQuoteUsd);
                    instrumentsToGetUsd.remove(quote);
//                    log.error("USD price of " + quote + " is estimated at " + priceOfQuoteUsd + " because " + base +
//                            "/" + quote + "=" + priceOfBaseQuote + " and " + base + "=" + instrumentUsdValue.get(base));
                }
            }
        }
        return instrumentUsdValue;
    }

    private double afterFee(double turnover) {
        if(!feesIncluded)
            return turnover;

        return (1 - feePct / 100) * turnover;
    }

    private Map<Instrument, Double> computeVolumes(List<Instrument> instrumentList, int volumeDays,
                                                   Map<String, Double> quoteToUsdPrice) {
        return instrumentList.parallelStream()
                .collect(Collectors.toMap(
                        i -> i,
                        i -> this.exchangeRunner.getQuoteVolume(i, volumeDays) * quoteToUsdPrice.get(i.getQuoteCurrency())
                ));
    }

}
