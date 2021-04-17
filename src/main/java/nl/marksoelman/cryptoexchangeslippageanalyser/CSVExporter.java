package nl.marksoelman.cryptoexchangeslippageanalyser;

import com.google.common.math.Quantiles;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import nl.marksoelman.cryptoexchangeslippageanalyser.model.Instrument;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service @Slf4j
public class CSVExporter {

    @Value("${core.export.file}")
    private String exportFile;

    @Value("${core.export.as-percent}")
    private Boolean exportAsPercent;

    public void export(HashMap<Instrument, List<Double>> instrumentSlippagePct,
                       Optional<Stream<Instrument>> sortedInstruments) {
        Map<Instrument, Double> instrumentAverage = aggregateValues(instrumentSlippagePct, CSVExporter::listToAvg);
        Map<Instrument, Double> instrumentMin = aggregateValues(instrumentSlippagePct, CSVExporter::listToMin);
        Map<Instrument, Double> instrument25Percentile = aggregateValues(instrumentSlippagePct,
                i -> listToQuartile(i, 1));
        Map<Instrument, Double> instrument50Percentile = aggregateValues(instrumentSlippagePct,
                i -> listToQuartile(i, 2));
        Map<Instrument, Double> instrument75Percentile = aggregateValues(instrumentSlippagePct,
                i -> listToQuartile(i, 3));
        Map<Instrument, Double> instrumentMax = aggregateValues(instrumentSlippagePct, CSVExporter::listToMax);

        double presentationMultiplier = (exportAsPercent) ? 100 : 1;

        BufferedWriter bw = prepareFile();
        try {
            bw.write("Instrument,Average,Min,25Percentile,50Percentile,75Percentile,Max");
            bw.newLine();
            sortedInstruments
                    .orElse(instrumentAverage.keySet().stream().sorted())
                    .forEach(instrument -> {
                        try {
                            bw.write(instrument.toString() + ",");
                            bw.write(presentationMultiplier*instrumentAverage.get(instrument) + ",");
                            bw.write(presentationMultiplier*instrumentMin.get(instrument) + ",");
                            bw.write(presentationMultiplier*instrument25Percentile.get(instrument) + ",");
                            bw.write(presentationMultiplier*instrument50Percentile.get(instrument) + ",");
                            bw.write(presentationMultiplier*instrument75Percentile.get(instrument) + ",");
                            bw.write(Double.toString(presentationMultiplier*instrumentMax.get(instrument)));
                            bw.newLine();
                        } catch (IOException e2) {
                            e2.printStackTrace();
                        }
                    });
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                bw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @SneakyThrows
    private BufferedWriter prepareFile() {
        return new BufferedWriter(new FileWriter(exportFile));
    }

    private Map<Instrument, Double> aggregateValues(Map<Instrument, List<Double>> instrumentSlippagePct,
                                                    Function<List<Double>, Double> mapper) {
        return instrumentSlippagePct.entrySet()
                .stream()
                .map(instrumentSlippages -> new AbstractMap.SimpleEntry<>(
                        instrumentSlippages.getKey(),
                        mapper.apply(instrumentSlippages.getValue())
                ))
                .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));
    }

    private static double listToAvg(List<Double> list) {
        return list.stream().mapToDouble(x -> x).average().orElse(Double.MIN_VALUE);
    }

    private static double listToMin(List<Double> list) {
        return list.stream().mapToDouble(x -> x).min().orElse(Double.MAX_VALUE);
    }

    private static double listToQuartile(List<Double> list, int quartile) {
        return Quantiles.percentiles().index(quartile * 25).compute(list);
    }

    private static double listToMax(List<Double> list) {
        return list.stream().mapToDouble(x -> x).max().orElse(Double.MIN_VALUE);
    }
}
