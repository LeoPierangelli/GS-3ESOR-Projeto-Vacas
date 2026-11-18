package br.com.fiap.ordenha;

import br.com.fiap.ordenha.config.RegrasAlertaProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(RegrasAlertaProperties.class)
public class OrdenhaApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrdenhaApiApplication.class, args);
    }
}
