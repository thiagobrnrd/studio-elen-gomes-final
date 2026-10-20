package com.studioelengomes.StudioElenGomes.config;

import com.studioelengomes.StudioElenGomes.repository.ClienteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DbStartupLogger implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DbStartupLogger.class);
    private final ClienteRepository clientes;
    private final String url;

    public DbStartupLogger(ClienteRepository clientes, @Value("${spring.datasource.url}") String url) {
        this.clientes = clientes;
        this.url = url;
    }

    @Override
    public void run(String... args) {
        String safe = url.replaceAll("//[^/@]*@", "//***@").replaceAll("([?&])(password|user)=[^&]*", "$1$2=***");
        log.info("Banco conectado: {}", safe);
        log.info("Clientes encontrados no banco: {}", clientes.count());
    }
}
