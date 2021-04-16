package nl.marksoelman.cryptoexchangeslippageanalyser.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @AllArgsConstructor @NoArgsConstructor
public class Instrument {

    private String baseCurrency;
    private String quoteCurrency;

    public String getSymbolConcat() {
        return baseCurrency + quoteCurrency;
    }

    @Override
    public String toString() {
        return baseCurrency + "/" + quoteCurrency;
    }
}
