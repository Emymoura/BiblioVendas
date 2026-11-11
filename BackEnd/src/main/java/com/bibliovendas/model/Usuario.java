package com.bibliovendas.model;

public abstract class Usuario {
    private Long id;
    private String nome;
    private String email;
    private String senha;
    private String endereco;

    protected Usuario(Long id, String nome, String email, String senha, String endereco) {
        this(id, nome, email, SenhaUtil.gerarHash(senha), endereco, true);
    }

    protected Usuario(Long id, String nome, String email, String senha, String endereco,
                      boolean senhaJaCriptografada) {
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.senha = senhaJaCriptografada ? senha : SenhaUtil.gerarHash(senha);
        this.endereco = endereco;
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean autenticar(String senhaInformada) {
        return SenhaUtil.verificar(senhaInformada, senha);
    }

    public void atualizarDados(String nome, String email, String endereco) {
        this.nome = nome;
        this.email = email;
        this.endereco = endereco;
    }

    public String getEndereco() {
        return endereco;
    }

    public void setEndereco(String endereco) {
        this.endereco = endereco;
    }
}
