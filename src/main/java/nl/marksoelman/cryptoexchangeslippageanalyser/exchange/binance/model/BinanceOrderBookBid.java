package nl.marksoelman.cryptoexchangeslippageanalyser.exchange.binance.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BinanceOrderBookBid {

    private String price;
    private String quantity;

    @JsonCreator
    public BinanceOrderBookBid(String[] priceQuantityArray) {
        this.price = priceQuantityArray[0];
        this.quantity = priceQuantityArray[1];
    }

}
