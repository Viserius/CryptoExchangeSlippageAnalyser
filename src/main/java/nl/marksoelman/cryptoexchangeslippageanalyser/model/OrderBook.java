package nl.marksoelman.cryptoexchangeslippageanalyser.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data @AllArgsConstructor @NoArgsConstructor
public class OrderBook {

    private Instrument instrument;
    private List<OrderBookBid> bids;
    private List<OrderBookAsk> asks;

    public Double getMidpointPrice() {
        if(bids.size() == 0 || asks.size() == 0) {
            throw new IllegalStateException("Trying to get midpoint price of " + instrument +
                    " while bids size is: " + bids.size() + " and asks size is: " + asks.size());
        }

        OrderBookBid lowestBid = bids.get(0);
        OrderBookAsk highestAsk = asks.get(0);
        return (lowestBid.getPrice() * lowestBid.getQuantity() + highestAsk.getPrice() * highestAsk.getQuantity()) /
                (lowestBid.getQuantity() + highestAsk.getQuantity());
    }

    public boolean nonEmpty() {
        return bids.size() > 0 && asks.size() > 0;
    }

}
