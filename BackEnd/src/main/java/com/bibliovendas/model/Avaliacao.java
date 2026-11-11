package com.bibliovendas.model;

public class Avaliacao {
    private final Long livroId;
    private final int nota;
    private final String comentario;

    public Avaliacao(Long livroId, int nota, String comentario) {
        this.livroId = livroId;
        this.nota = nota;
        this.comentario = comentario;
    }

    public Long getLivroId() {
        return livroId;
    }

    public int getNota() {
        return nota;
    }

    public String getComentario() {
        return comentario;
    }
}
