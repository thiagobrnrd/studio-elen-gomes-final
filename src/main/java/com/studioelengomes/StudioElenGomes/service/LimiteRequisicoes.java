package com.studioelengomes.StudioElenGomes.service;

import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LimiteRequisicoes {
    private final Map<String, Deque<Long>> acessos = new ConcurrentHashMap<>();

    public boolean permitir(String chave, int maximo, long janelaMs) {
        long agora = System.currentTimeMillis();
        Deque<Long> fila = acessos.computeIfAbsent(chave, k -> new ArrayDeque<>());
        synchronized (fila) {
            while (!fila.isEmpty() && agora - fila.peekFirst() > janelaMs) fila.pollFirst();
            if (fila.size() >= maximo) return false;
            fila.addLast(agora);
        }
        if (acessos.size() > 10_000) acessos.clear();
        return true;
    }
}
