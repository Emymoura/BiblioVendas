CREATE TABLE IF NOT EXISTS usuarios (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(150) NOT NULL,
    email VARCHAR(190) NOT NULL UNIQUE,
    senha_hash VARCHAR(255) NOT NULL,
    endereco VARCHAR(255) NOT NULL,
    tipo VARCHAR(30) NOT NULL
);

CREATE TABLE IF NOT EXISTS livros (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(200) NOT NULL,
    sinopse TEXT,
    categoria VARCHAR(100) NOT NULL,
    genero VARCHAR(100) NOT NULL,
    autor VARCHAR(150) NOT NULL,
    editora VARCHAR(150) NOT NULL,
    numero_paginas INT NOT NULL,
    ano_publicacao INT NOT NULL,
    preco DECIMAL(10, 2) NOT NULL,
    quantidade INT NOT NULL,
    disponivel BOOLEAN NOT NULL,
    digital BOOLEAN NOT NULL
);

CREATE TABLE IF NOT EXISTS pedidos (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    cliente_id BIGINT NOT NULL,
    data_criacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(30) NOT NULL,
    tipo_pedido VARCHAR(30) NOT NULL,
    pagamento_tipo VARCHAR(30) NOT NULL,
    pagamento_valor DECIMAL(10, 2) NOT NULL,
    CONSTRAINT fk_pedidos_cliente FOREIGN KEY (cliente_id) REFERENCES usuarios(id)
);

CREATE TABLE IF NOT EXISTS itens_pedido (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    pedido_id BIGINT NOT NULL,
    livro_id BIGINT NOT NULL,
    quantidade INT NOT NULL,
    preco_unitario DECIMAL(10, 2) NOT NULL,
    CONSTRAINT fk_itens_pedido FOREIGN KEY (pedido_id) REFERENCES pedidos(id) ON DELETE CASCADE,
    CONSTRAINT fk_itens_livro FOREIGN KEY (livro_id) REFERENCES livros(id)
);

CREATE TABLE IF NOT EXISTS avaliacoes (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    cliente_id BIGINT NOT NULL,
    livro_id BIGINT NOT NULL,
    nota INT NOT NULL,
    comentario TEXT,
    data_criacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_avaliacoes_cliente FOREIGN KEY (cliente_id) REFERENCES usuarios(id),
    CONSTRAINT fk_avaliacoes_livro FOREIGN KEY (livro_id) REFERENCES livros(id)
);

CREATE TABLE IF NOT EXISTS cartoes_cliente (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    cliente_id BIGINT NOT NULL,
    titular VARCHAR(150) NOT NULL,
    ultimos_quatro CHAR(4) NOT NULL,
    CONSTRAINT fk_cartoes_cliente FOREIGN KEY (cliente_id) REFERENCES usuarios(id) ON DELETE CASCADE
);

INSERT INTO livros (
    nome, sinopse, categoria, genero, autor, editora,
    numero_paginas, ano_publicacao, preco, quantidade, disponivel, digital
)
SELECT 'Clean Code', 'Práticas para escrever código legível e sustentável.',
       'Tecnologia', 'Programação', 'Robert C. Martin', 'Alta Books',
       464, 2008, 79.90, 10, TRUE, TRUE
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM livros WHERE nome = 'Clean Code');

INSERT INTO livros (
    nome, sinopse, categoria, genero, autor, editora,
    numero_paginas, ano_publicacao, preco, quantidade, disponivel, digital
)
SELECT 'O Pequeno Príncipe', 'Uma história sobre amizade e descobertas.',
       'Literatura', 'Fábula', 'Antoine de Saint-Exupéry', 'Dover',
       96, 1943, 29.90, 8, TRUE, FALSE
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM livros WHERE nome = 'O Pequeno Príncipe');

INSERT INTO livros (
    nome, sinopse, categoria, genero, autor, editora,
    numero_paginas, ano_publicacao, preco, quantidade, disponivel, digital
)
SELECT 'Fundamentos de Java', 'Conceitos fundamentais de programação Java.',
       'Tecnologia', 'Programação', 'Cay S. Horstmann', 'Bookman',
       720, 2022, 119.90, 6, TRUE, TRUE
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM livros WHERE nome = 'Fundamentos de Java');
