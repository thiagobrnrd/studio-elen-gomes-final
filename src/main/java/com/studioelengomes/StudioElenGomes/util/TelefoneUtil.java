package com.studioelengomes.StudioElenGomes.util;

import java.util.Optional;

public final class TelefoneUtil {

    private TelefoneUtil() {
    }

    public static Optional<String> paraWhatsApp(String telefone) {
        if (telefone == null) {
            return Optional.empty();
        }

        String digitos = telefone.replaceAll("\\D", "");

        digitos = digitos.replaceFirst("^0+", "");

        if (digitos.startsWith("55") && (digitos.length() == 12 || digitos.length() == 13)) {
            digitos = digitos.substring(2);
        }

        if (digitos.length() != 10 && digitos.length() != 11) {
            return Optional.empty();
        }

        String ddd = digitos.substring(0, 2);
        String numero = digitos.substring(2);

        if (ddd.charAt(0) == '0' || ddd.charAt(1) == '0') {
            return Optional.empty();
        }

        if (numero.length() == 9) {
            if (numero.charAt(0) != '9') {
                return Optional.empty();
            }
        } else {
            char primeiro = numero.charAt(0);
            if (primeiro >= '6' && primeiro <= '9') {
                numero = "9" + numero;
            } else if (primeiro < '2' || primeiro > '5') {
                return Optional.empty();
            }
        }

        return Optional.of("55" + ddd + numero);
    }

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
