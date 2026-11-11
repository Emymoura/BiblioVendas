package com.bibliovendas.model;

import java.time.LocalDate;

public class Avaliacao {
    private final Long livroId;
    private final int nota;
    private final String comentario;
    private final String nomeCliente;
    private final LocalDate data;
    private final boolean demonstracao;

    public Avaliacao(Long livroId, int nota, String comentario) {
        this(livroId, nota, comentario, null, LocalDate.now(), false);
    }

    public Avaliacao(Long livroId, int nota, String comentario,
                     String nomeCliente, LocalDate data, boolean demonstracao) {
        this.livroId = livroId;
        this.nota = nota;
        this.comentario = comentario;
        this.nomeCliente = nomeCliente;
        this.data = data;
        this.demonstracao = demonstracao;
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

    public String getNomeCliente() {
        return nomeCliente;
    }

    public LocalDate getData() {
        return data;
    }

    public boolean isDemonstracao() {
        return demonstracao;
    }
}
