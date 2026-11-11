# Frontend BiblioVendas

Interface React conectada à API REST do backend. Catálogo, páginas de livros, carrinho de compras com vários títulos, busca, login/cadastro, histórico paginado, aluguéis, avaliações e solicitações de reembolso usam dados do MySQL através dos endpoints `/api`. O carrinho é preservado no navegador entre recargas; aluguéis continuam sendo pedidos individuais de livros digitais.

No cadastro, a senha precisa ter ao menos 8 caracteres, uma letra maiúscula, um número e um caractere especial. A tela mostra o progresso de cada requisito e permite exibir/ocultar a senha. Os aluguéis digitais duram 14 dias; reembolsos de compra são simulados e podem ser solicitados em até 7 dias.

A sessão é preservada no navegador entre recargas e validada com o backend ao abrir a aplicação. Como os tokens são mantidos em memória pelo backend, reiniciar a API encerra as sessões e exige novo login.

## Executar

Inicie o MySQL e o backend, depois em outro terminal:

```powershell
cd FrontEnd
npm install
npm run dev
```

Abra `http://localhost:3000`. O Vite encaminha as chamadas `/api` para `http://127.0.0.1:8080`. Para outra URL, configure `VITE_API_BASE_URL` antes do build.

```powershell
npm run build
```
