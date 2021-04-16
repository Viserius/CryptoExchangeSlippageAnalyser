package nl.marksoelman.cryptoexchangeslippageanalyser.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor
public class OrderBookAsk {

    private double price;
    private double quantity;

}
