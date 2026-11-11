import { useEffect, useMemo, useState } from 'react';

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
  const [avaliacoes, setAvaliacoes] = useState([]);
  const [erroAvaliacoes, setErroAvaliacoes] = useState('');
  const [modoCadastro, setModoCadastro] = useState(false);
  const [formulario, setFormulario] = useState({
    nome: '',
    email: '',
    senha: '',
    endereco: '',
  });
  const [sessao, setSessao] = useState(null);
  const [pagamento, setPagamento] = useState('PIX');
  const [mostrarHistorico, setMostrarHistorico] = useState(false);
  const [historico, setHistorico] = useState([]);
  const [carregandoHistorico, setCarregandoHistorico] = useState(false);
  const [erroHistorico, setErroHistorico] = useState('');
  const [atualizacaoHistorico, setAtualizacaoHistorico] = useState(0);
  const [mensagem, setMensagem] = useState('');
  const [erro, setErro] = useState('');

  const avaliacoesPorLivro = useMemo(() => {
    return avaliacoes.reduce((porLivro, avaliacao) => {
      const lista = porLivro.get(avaliacao.livroId) || [];
      lista.push(avaliacao);
      porLivro.set(avaliacao.livroId, lista);
      return porLivro;
    }, new Map());
  }, [avaliacoes]);

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

  useEffect(() => {
    const controller = new AbortController();
    setErroAvaliacoes('');
    apiRequest('/avaliacoes', { signal: controller.signal })
      .then(setAvaliacoes)
      .catch((error) => {
        if (error.name !== 'AbortError') setErroAvaliacoes(error.message);
      });
    return () => controller.abort();
  }, []);

  useEffect(() => {
    if (!sessao || !mostrarHistorico) return undefined;
    const controller = new AbortController();
    setCarregandoHistorico(true);
    setErroHistorico('');
    apiRequest('/pedidos', {
      signal: controller.signal,
      headers: { Authorization: `Bearer ${sessao.token}` },
    })
      .then(setHistorico)
      .catch((error) => {
        if (error.name !== 'AbortError') setErroHistorico(error.message);
      })
      .finally(() => {
        if (!controller.signal.aborted) setCarregandoHistorico(false);
      });
    return () => controller.abort();
  }, [sessao, mostrarHistorico, atualizacaoHistorico]);

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
      setMostrarHistorico(true);
      setAtualizacaoHistorico((atual) => atual + 1);
      const atualizado = await apiRequest(`/livros?busca=${encodeURIComponent(busca)}`);
      setLivros(atualizado);
    } catch (error) {
      setErro(error.message);
    }
  }

  async function enviarAvaliacao(event, livroId) {
    event.preventDefault();
    setErro('');
    setMensagem('');
    const form = event.currentTarget;
    const dados = new FormData(event.currentTarget);
    try {
      await apiRequest('/avaliacoes', {
        method: 'POST',
        headers: { Authorization: `Bearer ${sessao.token}` },
        body: JSON.stringify({
          livroId,
          nota: Number(dados.get('nota')),
          comentario: dados.get('comentario'),
        }),
      });
      setMensagem('Sua avaliação foi registrada.');
      setAvaliacoes(await apiRequest('/avaliacoes'));
      form.reset();
    } catch (error) {
      setErro(error.message);
    }
  }

  function sair() {
    setSessao(null);
    setMostrarHistorico(false);
    setHistorico([]);
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
                <button
                  type="button"
                  className="secondary-button"
                  onClick={() => setMostrarHistorico((atual) => !atual)}
                >
                  {mostrarHistorico ? 'Ocultar histórico' : 'Meu histórico'}
                </button>
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

        {sessao && mostrarHistorico && (
          <section id="historico" className="history-section">
            <div className="section-header">
              <h3>Meu histórico</h3>
              <span>{historico.length} pedido(s)</span>
            </div>
            {carregandoHistorico && <p>Carregando histórico...</p>}
            {erroHistorico && <p className="notice-error">{erroHistorico}</p>}
            {!carregandoHistorico && !erroHistorico && historico.length === 0 && (
              <p>Você ainda não fez compras ou aluguéis.</p>
            )}
            <div className="history-list">
              {historico.map((pedido) => (
                <article key={pedido.id} className="history-card">
                  <div className="history-heading">
                    <strong>Pedido #{pedido.id}</strong>
                    <span>{pedido.tipo === 'ALUGUEL' ? 'Aluguel' : 'Compra'} · {pedido.status}</span>
                  </div>
                  <p>
                    {pedido.data} · Pagamento: {pedido.pagamento} · Total: R${' '}
                    {Number(pedido.valorTotal).toFixed(2)}
                  </p>
                  <ul>
                    {pedido.itens.map((item) => (
                      <li key={`${pedido.id}-${item.livroId}`}>
                        {item.nome} × {item.quantidade}
                      </li>
                    ))}
                  </ul>
                </article>
              ))}
            </div>
          </section>
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
                {(() => {
                  const listaAvaliacoes = avaliacoesPorLivro.get(livro.id) || [];
                  const media = listaAvaliacoes.length
                    ? listaAvaliacoes.reduce((soma, item) => soma + item.nota, 0) / listaAvaliacoes.length
                    : 0;
                  return (
                    <details className="reviews-section">
                      <summary>
                        {listaAvaliacoes.length
                          ? `★ ${media.toFixed(1)} · ${listaAvaliacoes.length} avaliação(ões)`
                          : 'Sem avaliações'}
                      </summary>
                      {erroAvaliacoes && <p className="notice-error">{erroAvaliacoes}</p>}
                      <div className="review-list">
                        {listaAvaliacoes.map((avaliacao, indice) => (
                          <div key={`${livro.id}-${avaliacao.cliente}-${indice}`} className="review-item">
                            <strong>{avaliacao.cliente || 'Cliente'} · {'★'.repeat(avaliacao.nota)}</strong>
                            <p>{avaliacao.comentario || 'Sem comentário.'}</p>
                            {avaliacao.demonstracao && (
                              <small>Avaliação demonstrativa</small>
                            )}
                          </div>
                        ))}
                      </div>
                      {sessao && (
                        <form className="review-form" onSubmit={(event) => enviarAvaliacao(event, livro.id)}>
                          <label>
                            Sua nota
                            <select name="nota" defaultValue="5">
                              <option value="5">5 estrelas</option>
                              <option value="4">4 estrelas</option>
                              <option value="3">3 estrelas</option>
                              <option value="2">2 estrelas</option>
                              <option value="1">1 estrela</option>
                            </select>
                          </label>
                          <textarea name="comentario" placeholder="Escreva um comentário (opcional)" rows="2" />
                          <button type="submit">Avaliar</button>
                        </form>
                      )}
                      {!sessao && (
                        <p className="review-login-hint">Entre e compre ou alugue este livro para avaliá-lo.</p>
                      )}
                    </details>
                  );
                })()}
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
