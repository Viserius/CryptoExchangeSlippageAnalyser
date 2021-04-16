package nl.marksoelman.cryptoexchangeslippageanalyser.exchange.binance.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BinanceOrderBookAsk {

    private String price;
    private String quantity;

    @JsonCreator
    public BinanceOrderBookAsk(String[] priceQuantityArray) {
        this.price = priceQuantityArray[0];
        this.quantity = priceQuantityArray[1];
    }

}
