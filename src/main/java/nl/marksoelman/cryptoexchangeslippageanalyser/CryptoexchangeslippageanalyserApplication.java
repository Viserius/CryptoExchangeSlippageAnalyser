package nl.marksoelman.cryptoexchangeslippageanalyser;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Scanner;

@SpringBootApplication @Slf4j
public class CryptoexchangeslippageanalyserApplication {

	public static void main(String[] args) {
		ConfigurableApplicationContext context =
				SpringApplication.run(CryptoexchangeslippageanalyserApplication.class, args);

		Scanner scanner = new Scanner(System.in);
		log.info("Please enter an amount of USD to compute the slippage for.");
		double orderSize = scanner.nextDouble();

		context.getBean(SlippageRunner.class).run(orderSize);
	}

}
