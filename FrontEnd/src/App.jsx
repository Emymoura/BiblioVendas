import { useEffect, useMemo, useState } from 'react';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api';
const CHAVE_SESSAO = 'bibliovendas.session';
const CHAVE_CARRINHO = 'bibliovendas.cart';
const TAMANHO_PAGINA_PEDIDOS = 5;

function requisitosSenha(senha) {
  return [
    { texto: 'Pelo menos 8 caracteres', atende: Array.from(senha).length >= 8 },
    { texto: 'Uma letra maiúscula', atende: /\p{Lu}/u.test(senha) },
    { texto: 'Um número', atende: /\p{Nd}/u.test(senha) },
    { texto: 'Um caractere especial', atende: /[^\p{L}\p{Nd}]/u.test(senha) },
  ];
}

async function apiRequest(path, options = {}) {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...options.headers,
    },
  });
  if (response.status === 401 && options.headers?.Authorization) {
    window.dispatchEvent(new Event('bibliovendas:session-expirada'));
  }

  const textoResposta = await response.text();
  let data = null;
  if (textoResposta.trim()) {
    try {
      data = JSON.parse(textoResposta);
    } catch (error) {
      if (!(error instanceof SyntaxError)) throw error;
      if (response.ok) {
        throw new Error('A API retornou uma resposta inválida. Tente novamente.');
      }
    }
  }
  if (!response.ok) {
    const error = new Error(
      data?.erro
      || `A API respondeu com erro HTTP ${response.status}. Verifique se o backend está em execução.`
    );
    error.status = response.status;
    throw error;
  }
  if (data === null) {
    throw new Error('A API retornou uma resposta vazia. Verifique se o backend está em execução.');
  }
  return data;
}

function lerSessaoSalva() {
  let sessaoSalva;
  try {
    sessaoSalva = JSON.parse(window.localStorage.getItem(CHAVE_SESSAO));
  } catch {
    removerSessaoSalva();
    return null;
  }
  if (
    sessaoSalva
    && typeof sessaoSalva.token === 'string'
    && sessaoSalva.usuario
    && Number.isInteger(sessaoSalva.usuario.id)
  ) {
    return sessaoSalva;
  }
  removerSessaoSalva();
  return null;
}

function salvarSessao(sessao) {
  try {
    window.localStorage.setItem(CHAVE_SESSAO, JSON.stringify(sessao));
    return true;
  } catch {
    return false;
  }
}

function removerSessaoSalva() {
  try {
    window.localStorage.removeItem(CHAVE_SESSAO);
  } catch {
    return false;
  }
  return true;
}

function lerCarrinhoSalvo() {
  try {
    const carrinhoSalvo = JSON.parse(window.localStorage.getItem(CHAVE_CARRINHO));
    if (!Array.isArray(carrinhoSalvo)) return [];
    return carrinhoSalvo.filter((item) => (
      item?.livro
      && Number.isInteger(item.livro.id)
      && Number.isInteger(item.quantidade)
      && item.quantidade > 0
    ));
  } catch {
    return [];
  }
}

function App() {
  const [busca, setBusca] = useState('');
  const [livros, setLivros] = useState([]);
  const [carregando, setCarregando] = useState(true);
  const [erroCatalogo, setErroCatalogo] = useState('');
  const [avaliacoes, setAvaliacoes] = useState([]);
  const [erroAvaliacoes, setErroAvaliacoes] = useState('');
  const [modoCadastro, setModoCadastro] = useState(false);
  const [modoAdmin, setModoAdmin] = useState(false);
  const [mostrarSenha, setMostrarSenha] = useState(false);
  const [formulario, setFormulario] = useState({
    nome: '',
    email: '',
    senha: '',
    endereco: '',
  });
  const [sessao, setSessao] = useState(null);
  const [validandoSessao, setValidandoSessao] = useState(true);
  const [configuracao, setConfiguracao] = useState({
    duracaoAluguelDias: 14,
    formasPagamento: ['PIX', 'CARTAO', 'BOLETO'],
  });
  const [pagamento, setPagamento] = useState('PIX');
  const [carrinho, setCarrinho] = useState(lerCarrinhoSalvo);
  const [carrinhoAberto, setCarrinhoAberto] = useState(false);
  const [quantidadeLivro, setQuantidadeLivro] = useState(1);
  const [finalizandoCompra, setFinalizandoCompra] = useState(false);
  const [mostrarHistorico, setMostrarHistorico] = useState(false);
  const [secaoPedidos, setSecaoPedidos] = useState('historico');
  const [paginaPedidos, setPaginaPedidos] = useState(1);
  const [historico, setHistorico] = useState({
    pedidos: [],
    pagina: 1,
    limite: TAMANHO_PAGINA_PEDIDOS,
    total: 0,
    totalPaginas: 1,
  });
  const [carregandoHistorico, setCarregandoHistorico] = useState(false);
  const [erroHistorico, setErroHistorico] = useState('');
  const [atualizacaoHistorico, setAtualizacaoHistorico] = useState(0);
  const [livroSelecionado, setLivroSelecionado] = useState(null);
  const [carregandoLivro, setCarregandoLivro] = useState(false);
  const [mensagem, setMensagem] = useState('');
  const [erro, setErro] = useState('');
  const [novoLivro, setNovoLivro] = useState({
    nome: '',
    sinopse: '',
    categoria: '',
    genero: '',
    autor: '',
    editora: '',
    numeroPaginas: '',
    anoPublicacao: '',
    preco: '',
    quantidade: '1',
    digital: true,
  });
  const [estoqueAdmin, setEstoqueAdmin] = useState({});
  const [disponibilidadeAdmin, setDisponibilidadeAdmin] = useState({});

  useEffect(() => {
    const controller = new AbortController();
    const sessaoSalva = lerSessaoSalva();
    if (!sessaoSalva) {
      setValidandoSessao(false);
      return () => controller.abort();
    }

    apiRequest('/auth/session', {
      signal: controller.signal,
      headers: { Authorization: `Bearer ${sessaoSalva.token}` },
    })
      .then(({ usuario }) => {
        if (!controller.signal.aborted) {
          setSessao({ ...sessaoSalva, usuario });
        }
      })
      .catch((error) => {
        if (controller.signal.aborted) return;
        if (error.status === 401) {
          removerSessaoSalva();
          setSessao(null);
          setErro('Sua sessão expirou. Entre novamente para continuar.');
          return;
        }
        setSessao(sessaoSalva);
        setErro('Não foi possível validar a sessão agora. Tente novamente em instantes.');
      })
      .finally(() => {
        if (!controller.signal.aborted) setValidandoSessao(false);
      });

    return () => controller.abort();
  }, []);

  useEffect(() => {
    function limparSessaoExpirada() {
      removerSessaoSalva();
      setSessao(null);
      setMostrarHistorico(false);
      setLivroSelecionado(null);
      setErro('Sua sessão expirou. Entre novamente para continuar.');
    }
    window.addEventListener('bibliovendas:session-expirada', limparSessaoExpirada);
    return () => window.removeEventListener('bibliovendas:session-expirada', limparSessaoExpirada);
  }, []);

  const avaliacoesPorLivro = useMemo(() => {
    return avaliacoes.reduce((porLivro, avaliacao) => {
      const lista = porLivro.get(avaliacao.livroId) || [];
      lista.push(avaliacao);
      porLivro.set(avaliacao.livroId, lista);
      return porLivro;
    }, new Map());
  }, [avaliacoes]);

  const quantidadeCarrinho = useMemo(
    () => carrinho.reduce((total, item) => total + item.quantidade, 0),
    [carrinho]
  );
  const subtotalCarrinho = useMemo(
    () => carrinho.reduce(
      (total, item) => total + Number(item.livro.preco) * item.quantidade,
      0
    ),
    [carrinho]
  );

  useEffect(() => {
    try {
      window.localStorage.setItem(CHAVE_CARRINHO, JSON.stringify(carrinho));
    } catch {
      setErro('Não foi possível salvar o carrinho neste navegador.');
    }
  }, [carrinho]);

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
    const controller = new AbortController();
    apiRequest('/configuracoes', { signal: controller.signal })
      .then((dados) => {
        if (controller.signal.aborted) return;
        setConfiguracao(dados);
        setPagamento((atual) => (
          dados.formasPagamento.includes(atual) ? atual : dados.formasPagamento[0]
        ));
      })
      .catch((error) => {
        if (error.name !== 'AbortError') setErro(error.message);
      });
    return () => controller.abort();
  }, []);

  useEffect(() => {
    if (sessao?.usuario?.tipo !== 'ADMIN') return undefined;
    const controller = new AbortController();
    apiRequest('/admin/configuracoes', {
      signal: controller.signal,
      headers: { Authorization: `Bearer ${sessao.token}` },
    })
      .then(setConfiguracao)
      .catch((error) => {
        if (error.name !== 'AbortError') setErro(error.message);
      });
    return () => controller.abort();
  }, [sessao]);

  useEffect(() => {
    if (!sessao || !mostrarHistorico) return undefined;
    const controller = new AbortController();
    setCarregandoHistorico(true);
    setErroHistorico('');
    const filtroTipo = secaoPedidos === 'alugueis' ? '&tipo=ALUGUEL' : '';
    apiRequest(`/pedidos?pagina=${paginaPedidos}&limite=${TAMANHO_PAGINA_PEDIDOS}${filtroTipo}`, {
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
  }, [sessao, mostrarHistorico, secaoPedidos, paginaPedidos, atualizacaoHistorico]);

  const requisitos = requisitosSenha(formulario.senha);
  const senhaValida = requisitos.every((requisito) => requisito.atende);

  function atualizarCampo(event) {
    setFormulario((atual) => ({ ...atual, [event.target.name]: event.target.value }));
  }

  async function autenticar(event) {
    event.preventDefault();
    setErro('');
    setMensagem('');
    if (modoCadastro && !senhaValida) {
      setErro('A senha ainda não atende a todos os requisitos.');
      return;
    }
    try {
      const sessaoCriada = await apiRequest(
        modoCadastro ? '/clientes' : (modoAdmin ? '/admin/login' : '/auth/login'),
        {
          method: 'POST',
          body: JSON.stringify(formulario),
        }
      );
      setSessao(sessaoCriada);
      setMensagem(`Olá, ${sessaoCriada.usuario.nome}! Você está conectado.`);
      if (!salvarSessao(sessaoCriada)) {
        setErro('Não foi possível salvar sua sessão neste navegador.');
      }
      setFormulario((atual) => ({ ...atual, senha: '' }));
      setMostrarSenha(false);
    } catch (error) {
      setErro(error.message);
    }
  }

  async function salvarConfiguracoesAdmin(event) {
    event.preventDefault();
    setErro('');
    setMensagem('');
    try {
      const atualizada = await apiRequest('/admin/configuracoes', {
        method: 'PUT',
        headers: { Authorization: `Bearer ${sessao.token}` },
        body: JSON.stringify(configuracao),
      });
      setConfiguracao(atualizada);
      setMensagem('Configurações administrativas salvas.');
    } catch (error) {
      setErro(error.message);
    }
  }

  async function cadastrarLivroAdmin(event) {
    event.preventDefault();
    setErro('');
    setMensagem('');
    try {
      const livro = await apiRequest('/admin/livros', {
        method: 'POST',
        headers: { Authorization: `Bearer ${sessao.token}` },
        body: JSON.stringify({
          ...novoLivro,
          numeroPaginas: Number(novoLivro.numeroPaginas),
          anoPublicacao: Number(novoLivro.anoPublicacao),
          preco: Number(novoLivro.preco),
          quantidade: Number(novoLivro.quantidade),
          disponivel: Number(novoLivro.quantidade) > 0,
        }),
      });
      setLivros((atuais) => [...atuais, livro].sort((a, b) => a.nome.localeCompare(b.nome)));
      setNovoLivro({
        nome: '',
        sinopse: '',
        categoria: '',
        genero: '',
        autor: '',
        editora: '',
        numeroPaginas: '',
        anoPublicacao: '',
        preco: '',
        quantidade: '1',
        digital: true,
      });
      setMensagem(`Livro "${livro.nome}" cadastrado no catálogo.`);
    } catch (error) {
      setErro(error.message);
    }
  }

  async function salvarEstoqueAdmin(livro) {
    setErro('');
    setMensagem('');
    try {
      const atualizado = await apiRequest(`/admin/livros/${livro.id}/estoque`, {
        method: 'PUT',
        headers: { Authorization: `Bearer ${sessao.token}` },
        body: JSON.stringify({
          quantidade: Number(estoqueAdmin[livro.id] ?? livro.quantidade),
          disponivel: disponibilidadeAdmin[livro.id] ?? livro.disponivel,
        }),
      });
      setLivros((atuais) => atuais.map((item) => item.id === atualizado.id ? atualizado : item));
      setEstoqueAdmin((atual) => ({ ...atual, [atualizado.id]: atualizado.quantidade }));
      setDisponibilidadeAdmin((atual) => ({ ...atual, [atualizado.id]: atualizado.disponivel }));
      setMensagem(`Estoque de "${livro.nome}" atualizado.`);
    } catch (error) {
      setErro(error.message);
    }
  }

  async function desativarLivroAdmin(livro) {
    setErro('');
    setMensagem('');
    try {
      await apiRequest(`/admin/livros/${livro.id}/desativar`, {
        method: 'POST',
        headers: { Authorization: `Bearer ${sessao.token}` },
      });
      setLivros((atuais) => atuais.map((item) => (
        item.id === livro.id ? { ...item, disponivel: false } : item
      )));
      setDisponibilidadeAdmin((atual) => ({ ...atual, [livro.id]: false }));
      setMensagem(`"${livro.nome}" foi desativado no catálogo.`);
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
      setLivroSelecionado(null);
      setCarrinhoAberto(false);
      setMostrarHistorico(true);
      setSecaoPedidos(tipo === 'ALUGUEL' ? 'alugueis' : 'historico');
      setPaginaPedidos(1);
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

  async function abrirLivro(livro, pedido = null) {
    setCarrinhoAberto(false);
    setQuantidadeLivro(1);
    setLivroSelecionado({ livro, pedido });
    setCarregandoLivro(true);
    try {
      const detalhes = await apiRequest(`/livros/${livro.id}`);
      setLivroSelecionado((atual) => (
        atual?.livro.id === livro.id ? { ...atual, livro: detalhes } : atual
      ));
    } catch (error) {
      setErro(error.message);
    } finally {
      setCarregandoLivro(false);
    }
  }

  function adicionarAoCarrinho(livro, quantidade = 1) {
    setErro('');
    setMensagem('');
    const jaAdicionado = carrinho.find((item) => item.livro.id === livro.id)?.quantidade || 0;
    if (!livro.disponivel || quantidade < 1 || jaAdicionado + quantidade > livro.quantidade) {
      setErro(`Estoque insuficiente para "${livro.nome}".`);
      return false;
    }
    setCarrinho((atual) => {
      const existente = atual.find((item) => item.livro.id === livro.id);
      if (existente) {
        return atual.map((item) => (
          item.livro.id === livro.id
            ? { ...item, livro, quantidade: item.quantidade + quantidade }
            : item
        ));
      }
      return [...atual, { livro, quantidade }];
    });
    setMensagem(`${livro.nome} foi adicionado ao carrinho.`);
    return true;
  }

  function alterarQuantidadeCarrinho(livroId, quantidade) {
    const item = carrinho.find((produto) => produto.livro.id === livroId);
    if (!item) return;
    if (quantidade < 1) {
      setCarrinho((atual) => atual.filter((produto) => produto.livro.id !== livroId));
      return;
    }
    if (quantidade > item.livro.quantidade) {
      setErro(`Estoque disponível: ${item.livro.quantidade}.`);
      return;
    }
    setErro('');
    setCarrinho((atual) => atual.map((produto) => (
      produto.livro.id === livroId ? { ...produto, quantidade } : produto
    )));
  }

  async function finalizarCompra() {
    setErro('');
    setMensagem('');
    if (!sessao) {
      setErro('Entre ou faça seu cadastro para finalizar a compra.');
      return;
    }
    if (carrinho.length === 0) {
      setErro('Adicione ao menos um livro ao carrinho.');
      return;
    }
    setFinalizandoCompra(true);
    try {
      const pedido = await apiRequest('/pedidos', {
        method: 'POST',
        headers: { Authorization: `Bearer ${sessao.token}` },
        body: JSON.stringify({
          itens: carrinho.map((item) => ({
            livroId: item.livro.id,
            quantidade: item.quantidade,
          })),
          tipo: 'COMPRA',
          pagamento,
        }),
      });
      setCarrinho([]);
      setCarrinhoAberto(false);
      setMostrarHistorico(true);
      setSecaoPedidos('historico');
      setPaginaPedidos(1);
      setAtualizacaoHistorico((atual) => atual + 1);
      setMensagem(`Pedido #${pedido.id} concluído. Você comprou ${pedido.itens.length} livro(s).`);
      setLivros(await apiRequest(`/livros?busca=${encodeURIComponent(busca)}`));
    } catch (error) {
      setErro(error.message);
    } finally {
      setFinalizandoCompra(false);
    }
  }

  function abrirCarrinho() {
    setLivroSelecionado(null);
    setCarrinhoAberto(true);
  }

  async function reembolsarPedido(pedido) {
    if (!window.confirm(`Confirmar o reembolso simulado do pedido #${pedido.id}?`)) return;
    setErro('');
    setMensagem('');
    try {
      await apiRequest(`/pedidos/${pedido.id}/reembolso`, {
        method: 'POST',
        headers: { Authorization: `Bearer ${sessao.token}` },
      });
      setMensagem(`O pedido #${pedido.id} foi reembolsado. O estorno é simulado.`);
      setLivroSelecionado((atual) => (
        atual?.pedido?.id === pedido.id
          ? { ...atual, pedido: { ...atual.pedido, status: 'REEMBOLSADO' } }
          : atual
      ));
      setAtualizacaoHistorico((atual) => atual + 1);
    } catch (error) {
      setErro(error.message);
    }
  }

  function sair() {
    removerSessaoSalva();
    setSessao(null);
    setMostrarHistorico(false);
    setHistorico({
      pedidos: [],
      pagina: 1,
      limite: TAMANHO_PAGINA_PEDIDOS,
      total: 0,
      totalPaginas: 1,
    });
    setLivroSelecionado(null);
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
          {sessao?.usuario?.tipo !== 'ADMIN' && (
            <button type="button" className="cart-button" onClick={abrirCarrinho}>
              Carrinho <span>{quantidadeCarrinho}</span>
            </button>
          )}
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
                <h3>{sessao.usuario.tipo === 'ADMIN' ? 'Painel administrativo' : `Olá, ${sessao.usuario.nome}`}</h3>
                <p className="account-email">{sessao.usuario.email}</p>
                {sessao.usuario.tipo !== 'ADMIN' && (
                  <>
                    <button
                      type="button"
                      className="secondary-button"
                      onClick={() => {
                        setSecaoPedidos('historico');
                        setPaginaPedidos(1);
                        setMostrarHistorico(true);
                        setLivroSelecionado(null);
                      }}
                    >
                      Meu histórico
                    </button>
                    <button
                      type="button"
                      className="secondary-button"
                      onClick={() => {
                        setSecaoPedidos('alugueis');
                        setPaginaPedidos(1);
                        setMostrarHistorico(true);
                        setLivroSelecionado(null);
                      }}
                    >
                      Meus livros alugados
                    </button>
                  </>
                )}
                <button type="button" onClick={sair}>Sair</button>
              </>
            ) : validandoSessao ? (
              <p>Verificando sua sessão...</p>
            ) : (
              <form onSubmit={autenticar}>
                <h3>{modoCadastro ? 'Criar conta' : modoAdmin ? 'Acesso administrativo' : 'Entrar'}</h3>
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
                  type={mostrarSenha ? 'text' : 'password'}
                  value={formulario.senha}
                  onChange={atualizarCampo}
                  placeholder="Senha"
                  autoComplete={modoCadastro ? 'new-password' : 'current-password'}
                  minLength={modoCadastro ? 8 : undefined}
                  required
                />
                <button
                  className="text-button password-toggle"
                  type="button"
                  aria-label={mostrarSenha ? 'Ocultar senha' : 'Mostrar senha'}
                  aria-pressed={mostrarSenha}
                  onClick={() => setMostrarSenha((atual) => !atual)}
                >
                  {mostrarSenha ? 'Ocultar senha' : 'Mostrar senha'}
                </button>
                {modoCadastro && (
                  <div className="password-requirements" aria-live="polite">
                    <p>{requisitos.filter((requisito) => requisito.atende).length} de 4 requisitos atendidos</p>
                    <ul>
                      {requisitos.map((requisito) => (
                        <li
                          key={requisito.texto}
                          className={requisito.atende ? 'requirement-met' : 'requirement-missing'}
                        >
                          <span aria-hidden="true">{requisito.atende ? '✓' : '○'}</span>
                          {requisito.texto}
                        </li>
                      ))}
                    </ul>
                  </div>
                )}
                <button type="submit">{modoCadastro ? 'Cadastrar' : 'Entrar'}</button>
                {!modoAdmin && (
                  <button
                    className="text-button"
                    type="button"
                    onClick={() => {
                      setModoCadastro((atual) => !atual);
                      setMostrarSenha(false);
                      setErro('');
                    }}
                  >
                    {modoCadastro ? 'Já tenho uma conta' : 'Criar conta'}
                  </button>
                )}
                {!modoCadastro && (
                  <button
                    className="text-button"
                    type="button"
                    onClick={() => {
                      setModoAdmin((atual) => !atual);
                      setErro('');
                    }}
                  >
                    {modoAdmin ? 'Entrar como cliente' : 'Acesso administrativo'}
                  </button>
                )}
              </form>
            )}
          </div>
        </section>

        {(mensagem || erro) && (
          <div className={`notice ${erro ? 'notice-error' : 'notice-success'}`} role="status">
            {erro || mensagem}
          </div>
        )}

        {sessao?.usuario?.tipo === 'ADMIN' && (
          <section className="admin-dashboard">
            <div className="admin-heading">
              <span className="badge">ADMIN</span>
              <h3>Gerenciar biblioteca</h3>
              <p>Atualize o catálogo, o estoque, as formas de pagamento e o prazo de aluguel.</p>
            </div>

            <div className="admin-settings-grid">
              <form className="admin-card admin-settings" onSubmit={salvarConfiguracoesAdmin}>
                <h4>Configurações da loja</h4>
                <label>
                  Prazo de aluguel (dias)
                  <input
                    type="number"
                    min="1"
                    max="365"
                    required
                    value={configuracao.duracaoAluguelDias}
                    onChange={(event) => setConfiguracao((atual) => ({
                      ...atual,
                      duracaoAluguelDias: Number(event.target.value),
                    }))}
                  />
                </label>
                <fieldset>
                  <legend>Formas de pagamento habilitadas</legend>
                  {[
                    ['PIX', 'PIX'],
                    ['CARTAO', 'Cartão'],
                    ['BOLETO', 'Boleto'],
                  ].map(([valor, rotulo]) => (
                    <label className="admin-checkbox" key={valor}>
                      <input
                        type="checkbox"
                        checked={configuracao.formasPagamento.includes(valor)}
                        onChange={(event) => setConfiguracao((atual) => ({
                          ...atual,
                          formasPagamento: event.target.checked
                            ? [...atual.formasPagamento, valor]
                            : atual.formasPagamento.filter((forma) => forma !== valor),
                        }))}
                      />
                      {rotulo}
                    </label>
                  ))}
                </fieldset>
                <button type="submit">Salvar configurações</button>
              </form>

              <form className="admin-card admin-book-form" onSubmit={cadastrarLivroAdmin}>
                <h4>Cadastrar livro no catálogo</h4>
                <div className="admin-form-grid">
                  <input
                    name="nome"
                    placeholder="Título"
                    required
                    value={novoLivro.nome}
                    onChange={(event) => setNovoLivro((atual) => ({ ...atual, nome: event.target.value }))}
                  />
                  <input
                    name="autor"
                    placeholder="Autor"
                    required
                    value={novoLivro.autor}
                    onChange={(event) => setNovoLivro((atual) => ({ ...atual, autor: event.target.value }))}
                  />
                  <input
                    name="categoria"
                    placeholder="Categoria"
                    required
                    value={novoLivro.categoria}
                    onChange={(event) => setNovoLivro((atual) => ({ ...atual, categoria: event.target.value }))}
                  />
                  <input
                    name="genero"
                    placeholder="Gênero"
                    required
                    value={novoLivro.genero}
                    onChange={(event) => setNovoLivro((atual) => ({ ...atual, genero: event.target.value }))}
                  />
                  <input
                    name="editora"
                    placeholder="Editora"
                    required
                    value={novoLivro.editora}
                    onChange={(event) => setNovoLivro((atual) => ({ ...atual, editora: event.target.value }))}
                  />
                  <input
                    name="numeroPaginas"
                    type="number"
                    min="1"
                    placeholder="Páginas"
                    required
                    value={novoLivro.numeroPaginas}
                    onChange={(event) => setNovoLivro((atual) => ({ ...atual, numeroPaginas: event.target.value }))}
                  />
                  <input
                    name="anoPublicacao"
                    type="number"
                    placeholder="Ano de publicação"
                    required
                    value={novoLivro.anoPublicacao}
                    onChange={(event) => setNovoLivro((atual) => ({ ...atual, anoPublicacao: event.target.value }))}
                  />
                  <input
                    name="preco"
                    type="number"
                    min="0"
                    step="0.01"
                    placeholder="Preço"
                    required
                    value={novoLivro.preco}
                    onChange={(event) => setNovoLivro((atual) => ({ ...atual, preco: event.target.value }))}
                  />
                  <input
                    name="quantidade"
                    type="number"
                    min="0"
                    placeholder="Estoque"
                    required
                    value={novoLivro.quantidade}
                    onChange={(event) => setNovoLivro((atual) => ({ ...atual, quantidade: event.target.value }))}
                  />
                  <label className="admin-checkbox">
                    <input
                      name="digital"
                      type="checkbox"
                      checked={novoLivro.digital}
                      onChange={(event) => setNovoLivro((atual) => ({ ...atual, digital: event.target.checked }))}
                    />
                    Livro digital (pode ser alugado)
                  </label>
                </div>
                <textarea
                  name="sinopse"
                  placeholder="Sinopse"
                  rows="3"
                  value={novoLivro.sinopse}
                  onChange={(event) => setNovoLivro((atual) => ({ ...atual, sinopse: event.target.value }))}
                />
                <button type="submit">Cadastrar livro</button>
              </form>
            </div>

            <div className="admin-card admin-inventory">
              <div className="section-header">
                <h4>Catálogo e estoque</h4>
                <span>{livros.length} livro(s)</span>
              </div>
              <div className="admin-inventory-list">
                {livros.map((livro) => (
                  <div className="admin-inventory-row" key={livro.id}>
                    <div className="admin-book-name">
                      <strong>{livro.nome}</strong>
                      <span>{livro.categoria} · {livro.disponivel ? 'Disponível' : 'Desativado'}</span>
                    </div>
                    <label>
                      Estoque
                      <input
                        type="number"
                        min="0"
                        value={estoqueAdmin[livro.id] ?? livro.quantidade}
                        onChange={(event) => setEstoqueAdmin((atual) => ({
                          ...atual,
                          [livro.id]: event.target.value,
                        }))}
                      />
                    </label>
                    <label className="admin-checkbox">
                      <input
                        type="checkbox"
                        checked={disponibilidadeAdmin[livro.id] ?? livro.disponivel}
                        onChange={(event) => setDisponibilidadeAdmin((atual) => ({
                          ...atual,
                          [livro.id]: event.target.checked,
                        }))}
                      />
                      Disponível
                    </label>
                    <button type="button" onClick={() => salvarEstoqueAdmin(livro)}>
                      Atualizar estoque
                    </button>
                    {livro.disponivel && (
                      <button
                        type="button"
                        className="admin-secondary-button"
                        onClick={() => {
                          if (window.confirm(`Desativar "${livro.nome}" no catálogo?`)) {
                            desativarLivroAdmin(livro);
                          }
                        }}
                      >
                        Desativar
                      </button>
                    )}
                  </div>
                ))}
              </div>
            </div>
          </section>
        )}

        {sessao?.usuario?.tipo === 'CLIENTE' && mostrarHistorico && !livroSelecionado && !carrinhoAberto && (
          <section id="historico" className="history-section">
            <div className="section-header">
              <h3>{secaoPedidos === 'alugueis' ? 'Meus livros alugados' : 'Meu histórico'}</h3>
              <span>{historico.total} {secaoPedidos === 'alugueis' ? 'aluguel(is)' : 'pedido(s)'}</span>
            </div>
            {carregandoHistorico && <p>Carregando histórico...</p>}
            {erroHistorico && <p className="notice-error">{erroHistorico}</p>}
            {!carregandoHistorico && !erroHistorico && historico.pedidos.length === 0 && (
              <p>
                {secaoPedidos === 'alugueis'
                  ? 'Você ainda não fez aluguéis.'
                  : 'Você ainda não fez compras ou aluguéis.'}
              </p>
            )}
            <div className="history-list">
              {historico.pedidos.map((pedido) => (
                <article key={pedido.id} className="history-card">
                  <div className="history-heading">
                    <strong>Pedido #{pedido.id}</strong>
                    <span>
                      {pedido.tipo === 'ALUGUEL' ? 'Aluguel' : 'Compra'} · {pedido.status}
                    </span>
                  </div>
                  <p>
                    {pedido.data} · Pagamento: {pedido.pagamento} · Total: R${' '}
                    {Number(pedido.valorTotal).toFixed(2)}
                  </p>
                  {pedido.tipo === 'ALUGUEL' && (
                    <p className="rental-deadline">
                      Prazo do aluguel
                      {pedido.dataFimAluguel
                        ? ` · até ${pedido.dataFimAluguel}`
                        : ` · ${configuracao.duracaoAluguelDias} dias`}
                    </p>
                  )}
                  <ul>
                    {pedido.itens.map((item) => (
                      <li key={`${pedido.id}-${item.livroId}`}>
                        <button
                          type="button"
                          className="book-name-link"
                          onClick={() => abrirLivro(
                            { id: item.livroId, nome: item.nome },
                            pedido
                          )}
                        >
                          {item.nome}
                        </button>
                        {' '}× {item.quantidade}
                      </li>
                    ))}
                  </ul>
                  {pedido.tipo === 'COMPRA' && pedido.status === 'CONFIRMADO' && (
                    <button
                      type="button"
                      className="refund-link"
                      onClick={() => abrirLivro(
                        {
                          id: pedido.itens[0].livroId,
                          nome: pedido.itens[0].nome,
                        },
                        pedido
                      )}
                    >
                      Ver livro e opções de reembolso
                    </button>
                  )}
                </article>
              ))}
            </div>
            {historico.totalPaginas > 1 && (
              <nav className="pagination" aria-label="Paginação de pedidos">
                <button
                  type="button"
                  className="secondary-button"
                  disabled={paginaPedidos <= 1 || carregandoHistorico}
                  onClick={() => setPaginaPedidos((pagina) => pagina - 1)}
                >
                  Anterior
                </button>
                <span>Página {historico.pagina} de {historico.totalPaginas}</span>
                <button
                  type="button"
                  className="secondary-button"
                  disabled={paginaPedidos >= historico.totalPaginas || carregandoHistorico}
                  onClick={() => setPaginaPedidos((pagina) => pagina + 1)}
                >
                  Próxima
                </button>
              </nav>
            )}
          </section>
        )}

        {sessao?.usuario?.tipo === 'ADMIN' ? null : carrinhoAberto ? (
          <section className="cart-page">
            <div className="cart-page-heading">
              <div>
                <span className="eyebrow">Sua seleção</span>
                <h3>Carrinho</h3>
                <p>{quantidadeCarrinho} {quantidadeCarrinho === 1 ? 'livro' : 'livros'}</p>
              </div>
              <button type="button" className="secondary-button" onClick={() => setCarrinhoAberto(false)}>
                Continuar comprando
              </button>
            </div>
            {carrinho.length === 0 ? (
              <div className="empty-cart">
                <h4>Seu carrinho está vazio</h4>
                <p>Encontre um livro no catálogo e adicione-o aqui.</p>
                <button type="button" onClick={() => setCarrinhoAberto(false)}>Explorar catálogo</button>
              </div>
            ) : (
              <div className="cart-layout">
                <div className="cart-items">
                  {carrinho.map((item) => (
                    <article className="cart-item" key={item.livro.id}>
                      <div className="book-cover book-cover-small" aria-hidden="true">
                        <span>{item.livro.nome}</span>
                      </div>
                      <div className="cart-item-info">
                        <h4>{item.livro.nome}</h4>
                        <p>{item.livro.autor}</p>
                        <strong>R$ {Number(item.livro.preco).toFixed(2)}</strong>
                        <div className="quantity-control">
                          <button
                            type="button"
                            aria-label={`Diminuir quantidade de ${item.livro.nome}`}
                            onClick={() => alterarQuantidadeCarrinho(item.livro.id, item.quantidade - 1)}
                          >
                            −
                          </button>
                          <span>{item.quantidade}</span>
                          <button
                            type="button"
                            aria-label={`Aumentar quantidade de ${item.livro.nome}`}
                            disabled={item.quantidade >= item.livro.quantidade}
                            onClick={() => alterarQuantidadeCarrinho(item.livro.id, item.quantidade + 1)}
                          >
                            +
                          </button>
                        </div>
                      </div>
                      <div className="cart-item-total">
                        <strong>R$ {(Number(item.livro.preco) * item.quantidade).toFixed(2)}</strong>
                        <button
                          type="button"
                          className="text-button"
                          onClick={() => alterarQuantidadeCarrinho(item.livro.id, 0)}
                        >
                          Remover
                        </button>
                      </div>
                    </article>
                  ))}
                </div>
                <aside className="cart-summary">
                  <h4>Resumo do pedido</h4>
                  <div><span>Subtotal</span><strong>R$ {subtotalCarrinho.toFixed(2)}</strong></div>
                  <div><span>Entrega</span><strong>Grátis</strong></div>
                  <div className="cart-grand-total">
                    <span>Total</span><strong>R$ {subtotalCarrinho.toFixed(2)}</strong>
                  </div>
                  <label className="payment-choice">
                    Forma de pagamento
                    <select value={pagamento} onChange={(event) => setPagamento(event.target.value)}>
                      {configuracao.formasPagamento.map((forma) => (
                        <option value={forma} key={forma}>
                          {forma === 'CARTAO' ? 'Cartão' : forma}
                        </option>
                      ))}
                    </select>
                  </label>
                  <button
                    type="button"
                    className="checkout-button"
                    disabled={finalizandoCompra}
                    onClick={finalizarCompra}
                  >
                    {finalizandoCompra ? 'Finalizando...' : 'Finalizar compra'}
                  </button>
                  {!sessao && <p className="checkout-hint">Entre na sua conta para concluir o pedido.</p>}
                  <p className="checkout-hint">O pagamento é simulado para este projeto.</p>
                </aside>
              </div>
            )}
          </section>
        ) : livroSelecionado ? (
          <section className="book-detail-page">
            <button
              type="button"
              className="secondary-button back-button"
              onClick={() => setLivroSelecionado(null)}
            >
              Voltar ao catálogo
            </button>
            {carregandoLivro && <p>Carregando detalhes do livro...</p>}
            <div className="product-layout">
              <div className="book-cover book-cover-large" aria-hidden="true">
                <span>{livroSelecionado.livro.nome}</span>
                <small>{livroSelecionado.livro.autor}</small>
              </div>
              <article className="book-detail-card">
                <span className="badge">{livroSelecionado.livro.categoria}</span>
                <h3>{livroSelecionado.livro.nome}</h3>
                <p className="product-author">por {livroSelecionado.livro.autor || 'Autor não informado'}</p>
                <p>{livroSelecionado.livro.sinopse || 'Descrição não informada.'}</p>
                <ul className="product-metadata">
                  <li><strong>Editora</strong><span>{livroSelecionado.livro.editora || 'Não informada'}</span></li>
                  <li><strong>Gênero</strong><span>{livroSelecionado.livro.genero || 'Não informado'}</span></li>
                  <li><strong>Páginas</strong><span>{livroSelecionado.livro.numeroPaginas || 'Não informado'}</span></li>
                  <li><strong>Ano</strong><span>{livroSelecionado.livro.anoPublicacao || 'Não informado'}</span></li>
                </ul>
                {livroSelecionado.pedido && (
                  <div className="order-detail">
                  <h4>Seu pedido #{livroSelecionado.pedido.id}</h4>
                  <p>
                    {livroSelecionado.pedido.tipo === 'ALUGUEL' ? 'Aluguel' : 'Compra'}
                    {' · '}{livroSelecionado.pedido.status}
                  </p>
                  {livroSelecionado.pedido.tipo === 'ALUGUEL' && (
                    <p className="rental-deadline">
                      Prazo do aluguel
                      {livroSelecionado.pedido.dataFimAluguel
                        ? ` · até ${livroSelecionado.pedido.dataFimAluguel}`
                        : ` · ${configuracao.duracaoAluguelDias} dias`}
                    </p>
                  )}
                  {livroSelecionado.pedido.tipo === 'COMPRA'
                    && livroSelecionado.pedido.status === 'CONFIRMADO' && (
                    <div className="refund-panel">
                      <p>
                        Reembolso simulado disponível por até 7 dias após a compra.
                        Nenhum estorno real é enviado ao meio de pagamento.
                      </p>
                      <button
                        type="button"
                        className="refund-link"
                        onClick={() => reembolsarPedido(livroSelecionado.pedido)}
                      >
                        Solicitar reembolso
                      </button>
                    </div>
                  )}
                  </div>
                )}
              </article>
              <aside className="purchase-card">
                <span className="price-label">Preço</span>
                <strong className="product-price">
                  R$ {Number(livroSelecionado.livro.preco || 0).toFixed(2)}
                </strong>
                <p className={livroSelecionado.livro.disponivel ? 'stock-available' : 'stock-unavailable'}>
                  {livroSelecionado.livro.disponivel
                    ? `${livroSelecionado.livro.quantidade} em estoque`
                    : 'Indisponível no momento'}
                </p>
                <label className="quantity-select">
                  Quantidade
                  <input
                    type="number"
                    min="1"
                    max={livroSelecionado.livro.quantidade}
                    value={quantidadeLivro}
                    onChange={(event) => setQuantidadeLivro(Number(event.target.value))}
                  />
                </label>
                <button
                  type="button"
                  className="checkout-button"
                  disabled={!livroSelecionado.livro.disponivel}
                  onClick={() => adicionarAoCarrinho(livroSelecionado.livro, quantidadeLivro)}
                >
                  Adicionar ao carrinho
                </button>
                <button
                  type="button"
                  className="secondary-button"
                  disabled={!livroSelecionado.livro.disponivel}
                  onClick={() => {
                    if (adicionarAoCarrinho(livroSelecionado.livro, quantidadeLivro)) {
                      setLivroSelecionado(null);
                      setCarrinhoAberto(true);
                    }
                  }}
                >
                  Comprar agora
                </button>
                {livroSelecionado.livro.digital && (
                  <button
                    type="button"
                    className="secondary-button"
                    disabled={!livroSelecionado.livro.disponivel}
                    onClick={() => criarPedido(livroSelecionado.livro, 'ALUGUEL')}
                  >
                    Alugar por {configuracao.duracaoAluguelDias} dias
                  </button>
                )}
                <p className="checkout-hint">
                  Aluguel digital disponível por {configuracao.duracaoAluguelDias} dias.
                </p>
              </aside>
            </div>
          </section>
        ) : (
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
                <button
                  type="button"
                  className="book-details-link"
                  onClick={() => abrirLivro(livro)}
                >
                  Ver página do livro
                </button>
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
                    onClick={() => abrirLivro(livro)}
                  >
                    Ver detalhes e comprar
                  </button>
                  {livro.digital && (
                    <button
                      type="button"
                      className="secondary-button"
                      disabled={!livro.disponivel}
                      onClick={() => abrirLivro(livro)}
                    >
                      Ver opção de aluguel
                    </button>
                  )}
                </div>
              </article>
            ))}
          </div>
        </section>
        )}
      </main>
    </div>
  );
}

export default App;
