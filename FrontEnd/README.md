# Frontend BiblioVendas

Interface React conectada à API REST do backend. Catálogo, busca, login/cadastro, histórico de pedidos e avaliações usam dados do MySQL através dos endpoints `/api`.

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
