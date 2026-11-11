package com.bibliovendas.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class Catalogo {
    private final Map<Long, Livro> livros = new HashMap<>();

    public void adicionarLivro(Livro livro) {
        livros.put(livro.getId(), livro);
    }

    public List<Livro> listarLivros() {
        return Collections.unmodifiableList(new ArrayList<>(livros.values()));
    }

    public Optional<Livro> buscarPorId(Long id) {
        return Optional.ofNullable(livros.get(id));
    }

    public List<Livro> buscarPorTermo(String termo) {
        if (termo == null || termo.trim().isEmpty()) {
            return listarLivros();
        }

        String textoBusca = termo.toLowerCase();
        return livros.values().stream()
                .filter(livro -> livro.getNome().toLowerCase().contains(textoBusca)
                        || livro.getAutor().toLowerCase().contains(textoBusca)
                        || livro.getGenero().toLowerCase().contains(textoBusca)
                        || livro.getEditora().toLowerCase().contains(textoBusca)
                        || livro.getCategoria().toLowerCase().contains(textoBusca))
                .collect(Collectors.toList());
    }

    public boolean removerLivro(Long id) {
        return livros.remove(id) != null;
    }

    public void desativarLivro(Long id) {
        livros.get(id).setDisponivel(false);
    }
}
