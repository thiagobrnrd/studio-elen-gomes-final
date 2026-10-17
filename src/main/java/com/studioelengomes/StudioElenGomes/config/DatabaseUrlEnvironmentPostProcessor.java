package com.studioelengomes.StudioElenGomes.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Monta a conexão do banco a partir de DATABASE_URL.
 *
 * Aceita a string que o Neon mostra no painel
 * ("postgresql://usuario:senha@host/banco?sslmode=require") e converte para o
 * formato JDBC que o Spring precisa. Também aceita uma URL já em formato JDBC.
 *
 * Também lê o arquivo .env da raiz do projeto (ver carregarDotEnv).
 */
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment env, SpringApplication application) {
        List<Path> procurados = carregarDotEnv(env);

        String raw = firstNonBlank(env.getProperty("DATABASE_URL"), env.getProperty("spring.datasource.url"));
        if (raw == null) {
            StringBuilder onde = new StringBuilder();
            for (Path p : procurados) {
                onde.append("\n   - ").append(p).append(Files.isRegularFile(p) ? "  (encontrado, mas sem DATABASE_URL)" : "  (nao existe)");
            }
            throw new IllegalStateException("""

                    ==============================================================
                     Banco de dados nao configurado: DATABASE_URL nao encontrada.
                     Arquivos .env procurados:%s
                     Confira:
                      1) o arquivo se chama exatamente ".env" (e nao ".env.txt" ou ".env.example")
                      2) ele esta na mesma pasta do pom.xml
                      3) a linha comeca com DATABASE_URL=postgresql://...
                    ==============================================================""".formatted(onde));
        }
        raw = raw.trim();

        Map<String, Object> props = new HashMap<>();
        String jdbcUrl;
        String user = env.getProperty("DATABASE_USERNAME");
        String pass = env.getProperty("DATABASE_PASSWORD");

        if (raw.startsWith("postgres://") || raw.startsWith("postgresql://")) {
            URI uri = URI.create(raw.replaceFirst("^postgres(ql)?://", "http://"));
            String userInfo = uri.getRawUserInfo();
            if (userInfo != null) {
                String[] parts = userInfo.split(":", 2);
                user = decode(parts[0]);
                if (parts.length > 1) pass = decode(parts[1]);
            }
            String host = uri.getHost() + (uri.getPort() > 0 ? ":" + uri.getPort() : "");
            String db = uri.getPath() == null || uri.getPath().isBlank() ? "/neondb" : uri.getPath();
            jdbcUrl = "jdbc:postgresql://" + host + db + "?" + jdbcQuery(uri.getRawQuery(), host);
        } else if (raw.startsWith("jdbc:postgresql://")) {
            jdbcUrl = raw;
        } else {
            throw new IllegalStateException("DATABASE_URL inválida. Use a connection string do Neon (postgresql://...) "
                    + "ou o formato JDBC (jdbc:postgresql://...).");
        }

        props.put("spring.datasource.url", jdbcUrl);
        if (user != null && !user.isBlank()) props.put("spring.datasource.username", user);
        if (pass != null && !pass.isBlank()) props.put("spring.datasource.password", pass);

        env.getPropertySources().addFirst(new MapPropertySource("studioDatabaseUrl", props));
    }

    /**
     * Lê o .env da pasta atual (e da pasta pai, caso o programa seja iniciado de
     * dentro de uma subpasta). Aceita arquivos salvos pelo Bloco de Notas/PowerShell
     * (UTF-8 com BOM ou UTF-16), aspas nos valores e comentários com #.
     * Variáveis de ambiente de verdade continuam tendo prioridade sobre o .env.
     */
    private static List<Path> carregarDotEnv(ConfigurableEnvironment env) {
        Path dir = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        List<Path> candidatos = new ArrayList<>();
        candidatos.add(dir.resolve(".env"));
        if (dir.getParent() != null) candidatos.add(dir.getParent().resolve(".env"));

        for (Path arquivo : candidatos) {
            if (!Files.isRegularFile(arquivo)) continue;
            try {
                byte[] bytes = Files.readAllBytes(arquivo);
                String texto;
                if (bytes.length >= 2 && (bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xFE)) {
                    texto = new String(bytes, 2, bytes.length - 2, StandardCharsets.UTF_16LE);
                } else if (bytes.length >= 2 && (bytes[0] == (byte) 0xFE && bytes[1] == (byte) 0xFF)) {
                    texto = new String(bytes, 2, bytes.length - 2, StandardCharsets.UTF_16BE);
                } else {
                    texto = new String(bytes, StandardCharsets.UTF_8);
                }
                texto = texto.replace("\uFEFF", "");

                Map<String, Object> valores = new LinkedHashMap<>();
                for (String linha : texto.split("\\r?\\n")) {
                    String l = linha.trim();
                    if (l.isEmpty() || l.startsWith("#")) continue;
                    if (l.startsWith("export ")) l = l.substring(7).trim();
                    int eq = l.indexOf('=');
                    if (eq <= 0) continue;
                    String chave = l.substring(0, eq).trim();
                    String valor = l.substring(eq + 1).trim();
                    if (valor.length() >= 2 && ((valor.startsWith("\"") && valor.endsWith("\""))
                            || (valor.startsWith("'") && valor.endsWith("'")))) {
                        valor = valor.substring(1, valor.length() - 1);
                    }
                    valores.put(chave, valor);
                }
                // addLast: variáveis definidas no sistema/painel do deploy ganham do .env
                env.getPropertySources().addLast(new MapPropertySource("dotenv:" + arquivo, valores));
                System.out.println("[Studio] .env carregado: " + arquivo + " (" + valores.size() + " variaveis)");
                break;
            } catch (IOException e) {
                System.out.println("[Studio] Nao foi possivel ler " + arquivo + ": " + e.getMessage());
            }
        }
        return candidatos;
    }

    /** Mantém sslmode e descarta parâmetros que o driver JDBC não entende (ex.: channel_binding). */
    private static String jdbcQuery(String rawQuery, String host) {
        String sslmode = null;
        if (rawQuery != null) {
            for (String kv : rawQuery.split("&")) {
                String[] p = kv.split("=", 2);
                if (p[0].equals("sslmode") && p.length > 1) sslmode = p[1];
            }
        }
        if (sslmode == null) {
            boolean local = host.startsWith("localhost") || host.startsWith("127.0.0.1");
            sslmode = local ? "disable" : "require";
        }
        return "sslmode=" + sslmode;
    }

    private static String decode(String s) {
        return URLDecoder.decode(s, StandardCharsets.UTF_8);
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) if (v != null && !v.isBlank()) return v;
        return null;
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}