import { useEffect, useState } from 'react';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api';

async function apiRequest(path, options = {}) {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...options.headers,
    },
  });
  const data = await response.json();
  if (!response.ok) {
    throw new Error(data.erro || 'Não foi possível concluir a solicitação.');
  }
  return data;
}

function App() {
  const [busca, setBusca] = useState('');
  const [livros, setLivros] = useState([]);
  const [carregando, setCarregando] = useState(true);
  const [erroCatalogo, setErroCatalogo] = useState('');
  const [modoCadastro, setModoCadastro] = useState(false);
  const [formulario, setFormulario] = useState({
    nome: '',
    email: '',
    senha: '',
    endereco: '',
  });
  const [sessao, setSessao] = useState(null);
  const [pagamento, setPagamento] = useState('PIX');
  const [mensagem, setMensagem] = useState('');
  const [erro, setErro] = useState('');

  useEffect(() => {
    const controller = new AbortController();
    setCarregando(true);
    setErroCatalogo('');

    apiRequest(`/livros?busca=${encodeURIComponent(busca)}`, {
      signal: controller.signal,
      headers: {},
    })
      .then(setLivros)
      .catch((error) => {
        if (error.name !== 'AbortError') {
          setErroCatalogo(error.message);
        }
      })
      .finally(() => {
        if (!controller.signal.aborted) setCarregando(false);
      });

    return () => controller.abort();
  }, [busca]);

  function atualizarCampo(event) {
    setFormulario((atual) => ({ ...atual, [event.target.name]: event.target.value }));
  }

  async function autenticar(event) {
    event.preventDefault();
    setErro('');
    setMensagem('');
    try {
      const sessaoCriada = await apiRequest(
        modoCadastro ? '/clientes' : '/auth/login',
        {
          method: 'POST',
          body: JSON.stringify(formulario),
        }
      );
      setSessao(sessaoCriada);
      setMensagem(`Olá, ${sessaoCriada.usuario.nome}! Você está conectado.`);
      setFormulario((atual) => ({ ...atual, senha: '' }));
    } catch (error) {
      setErro(error.message);
    }
  }

  async function criarPedido(livro, tipo) {
    setErro('');
    setMensagem('');
    if (!sessao) {
      setErro('Entre ou faça seu cadastro antes de realizar um pedido.');
      return;
    }

    try {
      const pedido = await apiRequest('/pedidos', {
        method: 'POST',
        headers: { Authorization: `Bearer ${sessao.token}` },
        body: JSON.stringify({
          livroId: livro.id,
          quantidade: 1,
          tipo,
          pagamento,
        }),
      });
      setMensagem(
        `Pedido #${pedido.id} ${tipo === 'ALUGUEL' ? 'alugado' : 'comprado'} com sucesso.`
      );
      setBusca((atual) => atual);
      const atualizado = await apiRequest(`/livros?busca=${encodeURIComponent(busca)}`);
      setLivros(atualizado);
    } catch (error) {
      setErro(error.message);
    }
  }

  function sair() {
    setSessao(null);
    setMensagem('Você saiu da sua conta.');
    setErro('');
  }

  return (
    <div className="app-shell">
      <header className="topbar">
        <div>
          <p className="eyebrow">Biblioteca digital</p>
          <h1>BiblioVendas</h1>
        </div>
        <nav>
          <a href="#catalogo">Catálogo</a>
          <a href="#conta">{sessao ? 'Minha conta' : 'Entrar'}</a>
        </nav>
      </header>

      <main className="content">
        <section className="hero">
          <div>
            <span className="badge">Sistema de venda e aluguel</span>
            <h2>Livros físicos e digitais para compra e aluguel.</h2>
            <p>
              Explore o catálogo, consulte detalhes e faça pedidos. Os dados são carregados e
              salvos no banco MySQL pelo backend.
            </p>
          </div>
          <div id="conta" className="login-card">
            {sessao ? (
              <>
                <h3>Olá, {sessao.usuario.nome}</h3>
                <p className="account-email">{sessao.usuario.email}</p>
                <button type="button" onClick={sair}>Sair</button>
              </>
            ) : (
              <form onSubmit={autenticar}>
                <h3>{modoCadastro ? 'Criar conta' : 'Entrar'}</h3>
                {modoCadastro && (
                  <>
                    <input
                      name="nome"
                      value={formulario.nome}
                      onChange={atualizarCampo}
                      placeholder="Nome"
                      autoComplete="name"
                      required
                    />
                    <input
                      name="endereco"
                      value={formulario.endereco}
                      onChange={atualizarCampo}
                      placeholder="Endereço"
                      autoComplete="street-address"
                      required
                    />
                  </>
                )}
                <input
                  name="email"
                  type="email"
                  value={formulario.email}
                  onChange={atualizarCampo}
                  placeholder="E-mail"
                  autoComplete="email"
                  required
                />
                <input
                  name="senha"
                  type="password"
                  value={formulario.senha}
                  onChange={atualizarCampo}
                  placeholder="Senha"
                  autoComplete={modoCadastro ? 'new-password' : 'current-password'}
                  minLength={4}
                  required
                />
                <button type="submit">{modoCadastro ? 'Cadastrar' : 'Entrar'}</button>
                <button
                  className="text-button"
                  type="button"
                  onClick={() => {
                    setModoCadastro((atual) => !atual);
                    setErro('');
                  }}
                >
                  {modoCadastro ? 'Já tenho uma conta' : 'Criar conta'}
                </button>
              </form>
            )}
          </div>
        </section>

        {(mensagem || erro) && (
          <div className={`notice ${erro ? 'notice-error' : 'notice-success'}`} role="status">
            {erro || mensagem}
          </div>
        )}

        <section id="catalogo" className="catalog-section">
          <div className="section-header">
            <h3>Catálogo</h3>
            <input
              value={busca}
              onChange={(event) => setBusca(event.target.value)}
              placeholder="Buscar por título, autor, gênero ou categoria"
              aria-label="Buscar livros"
            />
          </div>

          <label className="payment-choice">
            Forma de pagamento
            <select value={pagamento} onChange={(event) => setPagamento(event.target.value)}>
              <option value="PIX">PIX</option>
              <option value="CARTAO">Cartão</option>
              <option value="BOLETO">Boleto</option>
            </select>
          </label>

          {carregando && <p className="catalog-message">Carregando catálogo...</p>}
          {erroCatalogo && <p className="catalog-message notice-error">{erroCatalogo}</p>}
          {!carregando && !erroCatalogo && livros.length === 0 && (
            <p className="catalog-message">Nenhum livro encontrado no catálogo.</p>
          )}

          <div className="card-grid">
            {livros.map((livro) => (
              <article key={livro.id} className="book-card">
                <div className={`book-tag ${livro.disponivel ? '' : 'book-tag-unavailable'}`}>
                  {livro.disponivel ? 'Disponível' : 'Indisponível'}
                </div>
                <h4>{livro.nome}</h4>
                <p>{livro.sinopse || 'Descrição não informada.'}</p>
                <ul>
                  <li><strong>Autor:</strong> {livro.autor}</li>
                  <li><strong>Categoria:</strong> {livro.categoria}</li>
                  <li><strong>Gênero:</strong> {livro.genero}</li>
                  <li><strong>Páginas:</strong> {livro.numeroPaginas}</li>
                  <li><strong>Ano:</strong> {livro.anoPublicacao}</li>
                  <li><strong>Preço:</strong> R$ {Number(livro.preco).toFixed(2)}</li>
                </ul>
                <div className="book-actions">
                  <button
                    type="button"
                    disabled={!livro.disponivel}
                    onClick={() => criarPedido(livro, 'COMPRA')}
                  >
                    Comprar
                  </button>
                  {livro.digital && (
                    <button
                      type="button"
                      className="secondary-button"
                      disabled={!livro.disponivel}
                      onClick={() => criarPedido(livro, 'ALUGUEL')}
                    >
                      Alugar
                    </button>
                  )}
                </div>
              </article>
            ))}
          </div>
        </section>
      </main>
    </div>
  );
}

export default App;
