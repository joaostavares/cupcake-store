package br.com.cupcakesgourmet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class CupcakesGourmetApplication {

    public static void main(String[] args) {
        SpringApplication.run(CupcakesGourmetApplication.class, args);
    }
}
