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
    data_fim_aluguel DATE NULL,
    CONSTRAINT fk_pedidos_cliente FOREIGN KEY (cliente_id) REFERENCES usuarios(id)
);

CREATE TABLE IF NOT EXISTS itens_pedido (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    pedido_id BIGINT NOT NULL,
    livro_id BIGINT NOT NULL,
    nome_livro VARCHAR(200) NULL,
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

CREATE TABLE IF NOT EXISTS configuracoes_sistema (
    chave VARCHAR(80) NOT NULL PRIMARY KEY,
    valor VARCHAR(500) NOT NULL
);

INSERT INTO configuracoes_sistema (chave, valor)
VALUES ('duracao_aluguel_dias', '14')
ON DUPLICATE KEY UPDATE chave = VALUES(chave);

INSERT INTO configuracoes_sistema (chave, valor)
VALUES ('formas_pagamento', 'PIX,CARTAO,BOLETO')
ON DUPLICATE KEY UPDATE chave = VALUES(chave);

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

INSERT INTO livros (
    nome, sinopse, categoria, genero, autor, editora,
    numero_paginas, ano_publicacao, preco, quantidade, disponivel, digital
)
SELECT 'Dom Casmurro', 'Romance narrado por Bentinho, que revisita sua história e suas dúvidas.',
       'Literatura', 'Romance', 'Machado de Assis', 'Editora demonstrativa',
       256, 1899, 34.90, 7, TRUE, FALSE
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM livros WHERE nome = 'Dom Casmurro');

INSERT INTO livros (
    nome, sinopse, categoria, genero, autor, editora,
    numero_paginas, ano_publicacao, preco, quantidade, disponivel, digital
)
SELECT 'O Cortiço', 'Romance que retrata a vida coletiva em uma habitação no Rio de Janeiro.',
       'Literatura', 'Romance', 'Aluísio Azevedo', 'Editora demonstrativa',
       304, 1890, 32.90, 6, TRUE, FALSE
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM livros WHERE nome = 'O Cortiço');

INSERT INTO livros (
    nome, sinopse, categoria, genero, autor, editora,
    numero_paginas, ano_publicacao, preco, quantidade, disponivel, digital
)
SELECT 'Memórias Póstumas de Brás Cubas', 'Narrativa de um personagem que conta sua vida depois da morte.',
       'Literatura', 'Romance', 'Machado de Assis', 'Editora demonstrativa',
       240, 1881, 31.90, 5, TRUE, TRUE
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM livros WHERE nome = 'Memórias Póstumas de Brás Cubas');

INSERT INTO livros (
    nome, sinopse, categoria, genero, autor, editora,
    numero_paginas, ano_publicacao, preco, quantidade, disponivel, digital
)
SELECT 'A Moreninha', 'Romance ambientado na ilha de Paquetá e centrado em uma promessa juvenil.',
       'Literatura', 'Romance', 'Joaquim Manuel de Macedo', 'Editora demonstrativa',
       192, 1844, 27.90, 5, TRUE, FALSE
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM livros WHERE nome = 'A Moreninha');

INSERT INTO livros (
    nome, sinopse, categoria, genero, autor, editora,
    numero_paginas, ano_publicacao, preco, quantidade, disponivel, digital
)
SELECT 'Alice no País das Maravilhas', 'Uma aventura fantástica acompanha Alice por um mundo surpreendente.',
       'Literatura', 'Fantasia', 'Lewis Carroll', 'Editora demonstrativa',
       192, 1865, 28.90, 8, TRUE, TRUE
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM livros WHERE nome = 'Alice no País das Maravilhas');

INSERT INTO livros (
    nome, sinopse, categoria, genero, autor, editora,
    numero_paginas, ano_publicacao, preco, quantidade, disponivel, digital
)
SELECT 'A Arte da Guerra', 'Tratado clássico que apresenta princípios de estratégia e planejamento.',
       'História', 'Estratégia', 'Sun Tzu', 'Editora demonstrativa',
       160, -500, 24.90, 9, TRUE, TRUE
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM livros WHERE nome = 'A Arte da Guerra');

INSERT INTO livros (
    nome, sinopse, categoria, genero, autor, editora,
    numero_paginas, ano_publicacao, preco, quantidade, disponivel, digital
)
SELECT 'Frankenstein', 'Um experimento científico dá origem a uma reflexão sobre responsabilidade e pertencimento.',
       'Literatura', 'Ficção gótica', 'Mary Shelley', 'Editora demonstrativa',
       280, 1818, 33.90, 6, TRUE, FALSE
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM livros WHERE nome = 'Frankenstein');

INSERT INTO usuarios (nome, email, senha_hash, endereco, tipo)
SELECT 'Leitor demonstrativo', 'leitor1@bibliovendas.invalid',
       'conta-de-demonstracao-sem-login', 'Conta de demonstração', 'DEMO'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM usuarios WHERE email = 'leitor1@bibliovendas.invalid');

INSERT INTO usuarios (nome, email, senha_hash, endereco, tipo)
SELECT 'Clube de leitura demonstrativo', 'leitor2@bibliovendas.invalid',
       'conta-de-demonstracao-sem-login', 'Conta de demonstração', 'DEMO'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM usuarios WHERE email = 'leitor2@bibliovendas.invalid');

INSERT INTO avaliacoes (cliente_id, livro_id, nota, comentario)
SELECT usuario.id, livro.id, amostra.nota, amostra.comentario
FROM (
    SELECT 'Clean Code' AS titulo, 'leitor1@bibliovendas.invalid' AS email, 5 AS nota,
           'Exemplos claros para pensar na legibilidade do código.' AS comentario
    UNION ALL SELECT 'Clean Code', 'leitor2@bibliovendas.invalid', 4,
           'Boa referência para conversas sobre manutenção de software.'
    UNION ALL SELECT 'O Pequeno Príncipe', 'leitor1@bibliovendas.invalid', 5,
           'Uma leitura breve que rende boas conversas.'
    UNION ALL SELECT 'O Pequeno Príncipe', 'leitor2@bibliovendas.invalid', 4,
           'Uma história delicada para diferentes idades.'
    UNION ALL SELECT 'Fundamentos de Java', 'leitor1@bibliovendas.invalid', 4,
           'Conteúdo útil para acompanhar os primeiros estudos de Java.'
    UNION ALL SELECT 'Fundamentos de Java', 'leitor2@bibliovendas.invalid', 5,
           'Organização didática para consultar durante os exercícios.'
    UNION ALL SELECT 'Dom Casmurro', 'leitor1@bibliovendas.invalid', 5,
           'Uma narrativa envolvente e aberta a diferentes interpretações.'
    UNION ALL SELECT 'Dom Casmurro', 'leitor2@bibliovendas.invalid', 4,
           'Um clássico brasileiro que provoca boas discussões.'
    UNION ALL SELECT 'O Cortiço', 'leitor1@bibliovendas.invalid', 4,
           'Retrato marcante da vida urbana do período.'
    UNION ALL SELECT 'O Cortiço', 'leitor2@bibliovendas.invalid', 5,
           'Leitura interessante para estudar literatura brasileira.'
    UNION ALL SELECT 'Memórias Póstumas de Brás Cubas', 'leitor1@bibliovendas.invalid', 5,
           'Narrador original e observações cheias de ironia.'
    UNION ALL SELECT 'Memórias Póstumas de Brás Cubas', 'leitor2@bibliovendas.invalid', 4,
           'Um clássico que continua rendendo interpretações.'
    UNION ALL SELECT 'A Moreninha', 'leitor1@bibliovendas.invalid', 4,
           'Romance leve e interessante como registro de sua época.'
    UNION ALL SELECT 'A Moreninha', 'leitor2@bibliovendas.invalid', 5,
           'Boa leitura para conhecer um marco do romance brasileiro.'
    UNION ALL SELECT 'Alice no País das Maravilhas', 'leitor1@bibliovendas.invalid', 5,
           'Imaginação e situações curiosas do início ao fim.'
    UNION ALL SELECT 'Alice no País das Maravilhas', 'leitor2@bibliovendas.invalid', 4,
           'Uma aventura inventiva, com muitos detalhes para explorar.'
    UNION ALL SELECT 'A Arte da Guerra', 'leitor1@bibliovendas.invalid', 4,
           'Princípios concisos que podem ser discutidos em vários contextos.'
    UNION ALL SELECT 'A Arte da Guerra', 'leitor2@bibliovendas.invalid', 4,
           'Uma leitura curta sobre estratégia e planejamento.'
    UNION ALL SELECT 'Frankenstein', 'leitor1@bibliovendas.invalid', 5,
           'Uma história que convida a refletir sobre criação e responsabilidade.'
    UNION ALL SELECT 'Frankenstein', 'leitor2@bibliovendas.invalid', 4,
           'Clássico de ficção gótica com temas ainda atuais.'
) AS amostra
JOIN livros livro ON livro.nome = amostra.titulo
JOIN usuarios usuario ON usuario.email = amostra.email
WHERE NOT EXISTS (
    SELECT 1
    FROM avaliacoes existente
    WHERE existente.cliente_id = usuario.id
      AND existente.livro_id = livro.id
      AND existente.comentario = amostra.comentario
);
