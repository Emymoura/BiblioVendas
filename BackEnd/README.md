# Backend BiblioVendas

Backend Java com regras de negócio, persistência MySQL e API REST para o frontend. Senhas são armazenadas com PBKDF2; cartões cadastrados guardam apenas o titular e os quatro últimos dígitos.

## Banco de dados

Inicie um servidor MySQL e crie o banco/tabelas executando `database/schema.sql` no MySQL Workbench ou cliente MySQL. O script também insere um pequeno catálogo demonstrativo sem duplicar livros existentes. O backend executa o esquema novamente ao iniciar, para garantir que tabelas novas sejam criadas.

Configure as variáveis para conectar ao seu MySQL:

```powershell
$env:BIBLIO_DB_URL = "jdbc:mysql://127.0.0.1:3306/bibliovendas?serverTimezone=UTC"
$env:BIBLIO_DB_USER = "seu-usuario"
$env:BIBLIO_DB_PASSWORD = "sua-senha"
```

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
- `POST /api/clientes` — cadastro (`nome`, `email`, `senha`, `endereco`); retorna token de sessão
- `POST /api/auth/login` — login (`email`, `senha`); retorna token de sessão
- `GET /api/pedidos` — histórico do cliente autenticado
- `POST /api/pedidos` — compra ou aluguel (`livroId`, `quantidade`, `tipo`, `pagamento`)

Pedidos exigem o cabeçalho `Authorization: Bearer <token>`. Sessões ficam em memória e expiram ao reiniciar a API. Os pagamentos são simulados; nenhuma cobrança é enviada a uma operadora. O Vite encaminha `/api` ao backend durante o desenvolvimento.

## Estrutura

- `api` — servidor HTTP e rotas REST
- `model` — entidades do domínio e hash de senha
- `persistence` — conexão MySQL e repositório JDBC
- `service` — regras de negócio
- `database/schema.sql` — criação do banco, tabelas e catálogo de demonstração
- `src/test/java` — testes de unidade e API HTTP
