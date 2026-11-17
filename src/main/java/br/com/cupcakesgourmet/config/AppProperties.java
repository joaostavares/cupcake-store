package br.com.cupcakesgourmet.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Admin admin, Demo demo, Pix pix) {

    public record Admin(String email, String senha) {
    }

    public record Demo(boolean criarCliente, String email, String senha) {
    }

    public record Pix(String chave, String nomeRecebedor, String cidade) {
    }
}
