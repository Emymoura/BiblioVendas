package com.bibliovendas.model;

public class PrestadorServico extends Usuario {
    public PrestadorServico(Long id, String nome, String email, String senha, String endereco) {
        super(id, nome, email, senha, endereco);
    }

    public PrestadorServico(Long id, String nome, String email, String senha, String endereco,
                            boolean senhaJaCriptografada) {
        super(id, nome, email, senha, endereco, senhaJaCriptografada);
    }
}
