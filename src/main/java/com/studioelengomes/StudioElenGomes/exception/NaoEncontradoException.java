package com.studioelengomes.StudioElenGomes.exception;

/** Registro pedido pela API não existe (vira HTTP 404). */
public class NaoEncontradoException extends RuntimeException {
    public NaoEncontradoException(String message) {
        super(message);
    }
}
