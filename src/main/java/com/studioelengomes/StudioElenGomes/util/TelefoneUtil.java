package com.studioelengomes.StudioElenGomes.util;

import java.util.Optional;

/**
 * Converte telefones digitados à mão (ex.: "(11) 99910-1739") para o formato
 * internacional que o WhatsApp exige (ex.: "5511999101739").
 */
public final class TelefoneUtil {

    private TelefoneUtil() {
    }

    /**
     * Devolve o número no formato internacional (55 + DDD + número) ou vazio se o
     * telefone estiver incompleto/inválido.
     */
    public static Optional<String> paraWhatsApp(String telefone) {
        if (telefone == null) {
            return Optional.empty();
        }

        String digitos = telefone.replaceAll("\\D", "");

        // Remove zeros à esquerda (ex.: "0055...", "011 9...")
        digitos = digitos.replaceFirst("^0+", "");

        // Já vem com o código do país (55 + DDD + 8 ou 9 dígitos)
        if (digitos.startsWith("55") && (digitos.length() == 12 || digitos.length() == 13)) {
            digitos = digitos.substring(2);
        }

        // Agora precisa ser DDD (2) + número (8 ou 9)
        if (digitos.length() != 10 && digitos.length() != 11) {
            return Optional.empty();
        }

        String ddd = digitos.substring(0, 2);
        String numero = digitos.substring(2);

        if (ddd.charAt(0) == '0' || ddd.charAt(1) == '0') {
            return Optional.empty();
        }

        if (numero.length() == 9) {
            // Celular com nono dígito: precisa começar com 9
            if (numero.charAt(0) != '9') {
                return Optional.empty();
            }
        } else {
            // 8 dígitos: celular antigo (começa com 6 a 9) ganha o nono dígito;
            // fixo (começa com 2 a 5) fica como está.
            char primeiro = numero.charAt(0);
            if (primeiro >= '6' && primeiro <= '9') {
                numero = "9" + numero;
            } else if (primeiro < '2' || primeiro > '5') {
                return Optional.empty();
            }
        }

        return Optional.of("55" + ddd + numero);
    }

    /** Formata para exibição: "5511999101739" → "(11) 99910-1739". */
    public static String formatar(String internacional) {
        if (internacional == null || !internacional.startsWith("55")) {
            return internacional;
        }
        String ddd = internacional.substring(2, 4);
        String numero = internacional.substring(4);
        int corte = numero.length() - 4;
        return "(" + ddd + ") " + numero.substring(0, corte) + "-" + numero.substring(corte);
    }
}
