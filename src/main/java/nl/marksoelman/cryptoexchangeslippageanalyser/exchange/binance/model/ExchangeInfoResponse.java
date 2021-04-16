package nl.marksoelman.cryptoexchangeslippageanalyser.exchange.binance.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor @JsonIgnoreProperties(ignoreUnknown = true)
public class ExchangeInfoResponse {

    private List<SymbolResponse> symbols;

}
