# Backend BiblioVendas

Backend Java com regras de negócio, persistência MySQL e API REST para o frontend. Senhas são armazenadas com PBKDF2; cartões cadastrados guardam apenas o titular e os quatro últimos dígitos.

## Banco de dados

Inicie um servidor MySQL e crie o banco/tabelas executando `database/schema.sql` no MySQL Workbench ou cliente MySQL. O script insere dez livros e avaliações demonstrativas sem duplicar os dados existentes. O backend executa o esquema novamente ao iniciar.

Configure as variáveis para conectar ao seu MySQL:

```powershell
$env:BIBLIO_DB_URL = "jdbc:mysql://127.0.0.1:3306/bibliovendas?serverTimezone=UTC"
$env:BIBLIO_DB_USER = "seu-usuario"
$env:BIBLIO_DB_PASSWORD = "sua-senha"
```

Para criar o primeiro administrador, configure estas variáveis antes de iniciar a API. A conta é criada uma única vez; não existe cadastro público de ADMIN:

```powershell
$env:BIBLIO_ADMIN_EMAIL = "admin@admin.com"
$env:BIBLIO_ADMIN_PASSWORD = "Use-Uma-Senha-Forte-123!"
```

Use uma senha exclusiva com pelo menos 8 caracteres, maiúscula, número e símbolo. O backend armazena somente o hash PBKDF2. Se o e-mail já pertencer a um perfil que não seja ADMIN, a inicialização não o promoverá.

Na instalação local de demonstração, o e-mail ADMIN é `admin@admin.com`. A senha real não é registrada no README nem no repositório. As variáveis acima são usadas somente para criar o primeiro administrador; alterar essas variáveis não redefine uma conta já existente.

Opcionalmente, use Docker na pasta `BackEnd`:

```powershell
$env:MYSQL_ROOT_PASSWORD = "senha-local"
docker compose up -d
$env:BIBLIO_DB_USER = "root"
$env:BIBLIO_DB_PASSWORD = $env:MYSQL_ROOT_PASSWORD
```

## Executar

Com JDK 8+ e Maven instalados:

```powershell
cd BackEnd
mvn test
mvn exec:java "-Dexec.mainClass=com.bibliovendas.BibliotecaApplication"
```

A API fica em `http://127.0.0.1:8080/api` e permanece ativa até encerrar o processo. A porta pode ser alterada com `BIBLIO_API_PORT`.

## Endpoints REST

- `GET /api/health` — estado da API
- `GET /api/livros?busca=termo` — catálogo e busca
- `GET /api/livros/{id}` — detalhes de um livro
- `POST /api/clientes` — cadastro (`nome`, `email`, `senha`, `endereco`); exige 8 caracteres, maiúscula, número e caractere especial; retorna token
- `POST /api/auth/login` — login (`email`, `senha`); retorna token de sessão
- `POST /api/admin/login` — login administrativo; retorna token com perfil `ADMIN`
- `GET /api/auth/session` — valida a sessão autenticada
- `GET /api/configuracoes` — formas de pagamento habilitadas e prazo atual de aluguel
- `GET /api/admin/configuracoes` — configurações da loja, somente ADMIN
- `PUT /api/admin/configuracoes` — atualiza `duracaoAluguelDias` (1 a 365) e `formasPagamento` (PIX, CARTAO, BOLETO)
- `POST /api/admin/livros` — cadastra um livro no catálogo
- `PUT /api/admin/livros/{id}/estoque` — atualiza quantidade e disponibilidade
- `POST /api/admin/livros/{id}/desativar` — desativa um livro sem apagar pedidos anteriores
- `GET /api/pedidos` — histórico completo do cliente autenticado
- `GET /api/pedidos?pagina=1&limite=5&tipo=ALUGUEL` — página de pedidos; `tipo` também aceita `COMPRA`
- `POST /api/pedidos` — compra com vários itens (`itens: [{livroId, quantidade}]`, `tipo: "COMPRA"`, `pagamento`) ou aluguel de um livro (`livroId`, `quantidade: 1`, `tipo: "ALUGUEL"`, `pagamento`); o formato antigo de item único continua aceito
- `POST /api/pedidos/{id}/reembolso` — reembolso simulado de uma compra em até 7 dias; compras fora do prazo e aluguéis não podem ser reembolsados
- `GET /api/avaliacoes?livroId=1` — avaliações de um livro ou do catálogo inteiro
- `POST /api/avaliacoes` — avaliação autenticada (`livroId`, `nota`, `comentario`); exige compra ou aluguel

Pedidos, avaliações e operações administrativas exigem o cabeçalho `Authorization: Bearer <token>`. Rotas administrativas verificam o perfil no backend; contas de clientes não podem utilizá-las. Sessões ficam em memória e expiram ao reiniciar a API. As avaliações iniciais são identificadas como demonstrativas. Os pagamentos são simulados; nenhuma cobrança é enviada a uma operadora. O Vite encaminha `/api` ao backend durante o desenvolvimento.

Aluguéis são exclusivos por cliente e livro enquanto ativos e usam o prazo configurado pelo ADMIN (14 dias por padrão). A data de vencimento aparece no histórico e na página "Meus livros alugados". O backend migra automaticamente os pedidos antigos para salvar os nomes históricos dos livros e as datas de vencimento.

## Estrutura

- `api` — servidor HTTP e rotas REST
- `model` — entidades do domínio e hash de senha
- `persistence` — conexão MySQL e repositório JDBC
- `service` — regras de negócio
- `database/schema.sql` — criação do banco, tabelas e catálogo de demonstração
- `src/test/java` — testes de unidade e API HTTP
