package nl.marksoelman.cryptoexchangeslippageanalyser.exchange.binance.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @AllArgsConstructor @NoArgsConstructor @JsonIgnoreProperties(ignoreUnknown = true)
public class SymbolResponse {

    private String baseAsset;
    private String quoteAsset;
}
