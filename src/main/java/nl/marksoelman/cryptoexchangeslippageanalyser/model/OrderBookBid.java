package nl.marksoelman.cryptoexchangeslippageanalyser.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @AllArgsConstructor @NoArgsConstructor
public class OrderBookBid {

    private double price;
    private double quantity;

}
