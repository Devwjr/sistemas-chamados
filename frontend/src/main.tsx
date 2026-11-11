import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { createRoot } from 'react-dom/client';
import {
  Headphones,
  LayoutDashboard,
  Plus,
  Search,
  ArrowUpRight,
  LogOut,
  Ticket,
  Clock3,
  CircleCheck,
  Layers,
  Leaf,
  ChevronLeft,
  ChevronRight,
  X,
  Send,
  ShieldCheck,
} from 'lucide-react';
import '@fontsource/dm-sans/latin-400.css';
import '@fontsource/dm-sans/latin-500.css';
import '@fontsource/dm-sans/latin-600.css';
import '@fontsource/manrope/latin-600.css';
import '@fontsource/manrope/latin-700.css';
import '@fontsource/manrope/latin-800.css';
import './style.css';
import './operations.css';
import { AdminPanel } from './AdminPanel';
import { buildTicketQuery } from './ticketFilters';
type User = { id: number; nome: string; perfil: 'SOLICITANTE' | 'TECNICO' | 'ADMIN'; trocaSenhaObrigatoria: boolean };
type Ref = { id: number; nome: string };
type Dashboard = { total: number; porStatus: Record<string, number>; semResponsavel: number };
const statuses: Record<string, string> = {
  ABERTO: 'Aberto',
  EM_ATENDIMENTO: 'Em atendimento',
  AGUARDANDO_USUARIO: 'Aguardando resposta',
  RESOLVIDO: 'Resolvido',
  FECHADO: 'Fechado',
  CANCELADO: 'Cancelado',
};
type Ticket = {
  id: number;
  protocolo: string;
  titulo: string;
  descricao: string;
  status: string;
  prioridade: string;
  categoria: Ref;
  responsavel: Ref | null;
  criadoEm: string;
  versao: number;
  impacto: string;
  solucao: string | null;
  solicitante: Ref;
  setorOrigem: Ref;
};
type Page<T> = { items: T[]; totalPages: number };
type Comment = { id: number; autor: Ref; conteudo: string; visibilidade: 'PUBLICO' | 'INTERNO'; criadoEm: string };
type HistoryEvent = { id: number; autor: Ref; tipoEvento: string; valorAnterior: string | null; valorNovo: string | null; justificativa: string | null; criadoEm: string };
async function api<T>(path: string, method = 'GET', body?: object | URLSearchParams): Promise<T> {
  const headers = new Headers();
  if (method !== 'GET') {
    const r = await fetch('/api/auth/csrf');
    if (!r.ok) throw new Error('Não foi possível preparar a sessão.');
    const csrf = (await r.json()) as { headerName: string; token: string };
    headers.set(csrf.headerName, csrf.token);
    headers.set(
      'Content-Type',
      body instanceof URLSearchParams ? 'application/x-www-form-urlencoded' : 'application/json',
    );
  }
  const r = await fetch('/api' + path, {
    method,
    headers,
    body: body instanceof URLSearchParams ? body : body ? JSON.stringify(body) : undefined,
  });
  if (!r.ok) {
    const error = (await r.json().catch(() => ({}))) as { detail?: string; message?: string };
    throw new Error(error.detail || error.message || `Falha na solicitação (${r.status}).`);
  }
  return r.status === 204 ? (undefined as T) : r.json();
}
function App() {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [tickets, setTickets] = useState<Ticket[]>([]);
  const [categories, setCategories] = useState<Ref[]>([]);
  const [page, setPage] = useState(0);
  const [pages, setPages] = useState(0);
  const [dashboard, setDashboard] = useState<Dashboard | null>(null);
  const [search, setSearch] = useState('');
  const [status, setStatus] = useState('');
  const [priorityFilter, setPriorityFilter] = useState('');
  const [categoryFilter, setCategoryFilter] = useState('');
  const [openedFrom, setOpenedFrom] = useState('');
  const [openedTo, setOpenedTo] = useState('');
  const [responsibleFilter, setResponsibleFilter] = useState('');
  const [sectorFilter, setSectorFilter] = useState('');
  const [creating, setCreating] = useState(false);
  const [administration, setAdministration] = useState(false);
  const [notice, setNotice] = useState('');
  const [selected, setSelected] = useState<Ticket | null>(null);
  const [comments, setComments] = useState<Comment[]>([]);
  const [history, setHistory] = useState<HistoryEvent[]>([]);
  const [agents, setAgents] = useState<Ref[]>([]);
  const [detailLoading, setDetailLoading] = useState(false);
  const [detailError, setDetailError] = useState('');
  async function load(index = 0, query = '', filter = '', extraFilters: Record<string, string> = {}) {
    const params = buildTicketQuery(index, query, filter, extraFilters);
    const [list, cats, stats] = await Promise.all([
      api<Page<Ticket>>(`/chamados?${params}`),
      api<Page<Ref>>('/categorias?ativo=true&size=100'),
      api<Dashboard>('/painel'),
    ]);
    setDashboard(stats);
    setTickets(list.items);
    setPages(list.totalPages);
    setPage(index);
    setCategories(cats.items);
  }
  useEffect(() => {
    void api<User>('/auth/me')
      .then(async (current) => {
        setUser(current);
        if (!current.trocaSenhaObrigatoria) await load();
        if (!current.trocaSenhaObrigatoria && current.perfil !== 'SOLICITANTE') {
          const eligible = await api<Page<Ref>>('/usuarios/elegiveis-atendimento?size=100');
          setAgents(eligible.items);
        }
      })
      .catch(() => setUser(null))
      .finally(() => setLoading(false));
  }, []);
  async function submit(
    event: FormEvent<HTMLFormElement>,
    action: (data: FormData) => Promise<void>,
  ) {
    event.preventDefault();
    const form = event.currentTarget;
    setBusy(true);
    setError('');
    try {
      await action(new FormData(form));
      form.reset();
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Erro inesperado.');
    } finally {
      setBusy(false);
    }
  }
  async function refresh(index = 0, query = search, filter = status) {
    setBusy(true);
    setError('');
    try {
      await load(index, query, filter, {
        prioridade: priorityFilter,
        categoriaId: categoryFilter,
        de: openedFrom,
        ate: openedTo,
        responsavelId: responsibleFilter,
        setorId: sectorFilter,
      });
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Não foi possível carregar os chamados.');
    } finally {
      setBusy(false);
    }
  }
  async function openTicket(id: number) {
    setDetailLoading(true);
    setDetailError('');
    try {
      const [detail, commentPage, historyPage] = await Promise.all([
        api<Ticket>(`/chamados/${id}`),
        api<Page<Comment>>(`/chamados/${id}/comentarios?size=100`),
        api<Page<HistoryEvent>>(`/chamados/${id}/historico?size=100`),
      ]);
      setSelected(detail);
      setComments(commentPage.items);
      setHistory(historyPage.items);
      if (user?.perfil === 'ADMIN') {
        const eligible = await api<Page<Ref>>('/usuarios/elegiveis-atendimento?size=100');
        setAgents(eligible.items);
      }
    } catch (e) {
      setDetailError(e instanceof Error ? e.message : 'Não foi possível abrir o chamado.');
    } finally {
      setDetailLoading(false);
    }
  }
  async function mutateTicket(path: string, method: string, body: object) {
    if (!selected) return;
    setBusy(true);
    setDetailError('');
    try {
      await api(`/chamados/${selected.id}${path}`, method, { ...body, versao: selected.versao });
      await openTicket(selected.id);
      await load(page, search, status, {
        prioridade: priorityFilter,
        categoriaId: categoryFilter,
        de: openedFrom,
        ate: openedTo,
        responsavelId: responsibleFilter,
        setorId: sectorFilter,
      });
    } catch (e) {
      setDetailError(e instanceof Error ? e.message : 'Não foi possível atualizar o chamado.');
    } finally {
      setBusy(false);
    }
  }
  if (loading)
    return (
      <main className="loading">
        <Leaf size={32} />
        <p>Preparando seu espaço…</p>
      </main>
    );
  return (
    <div className={user ? 'app-shell' : 'auth-shell'}>
      {user && (
        <aside className="sidebar">
          <a className="brand" href="#">
            <span className="brand-icon">
              <Leaf size={23} />
            </span>
            <span>
              Núcleo<small>SUPORTE TI</small>
            </span>
          </a>
          <div className="workspace">
            <span className="workspace-icon">
              <Headphones size={18} />
            </span>
            <div>
              Central de atendimento<small>Seu espaço de trabalho</small>
            </div>
          </div>
          <span className="nav-caption">PRINCIPAL</span>
          <nav className="side-nav">
            <button className={!creating && !administration ? 'active' : ''} onClick={() => { setCreating(false); setAdministration(false); }}>
              <LayoutDashboard size={19} />
              Chamados
              <span className="nav-dot" />
            </button>
            <button className={creating ? 'active' : ''} onClick={() => { setAdministration(false); setCreating(true); }}>
              <Plus size={19} />
              Novo chamado
              <ArrowUpRight size={15} />
            </button>
            {user.perfil === 'ADMIN' && <button className={administration ? 'active' : ''} onClick={() => { setCreating(false); setAdministration(true); }}><ShieldCheck size={19} /> Administração</button>}
          </nav>
          <div className="side-help">
            <span className="help-icon">
              <Headphones size={21} />
            </span>
            <h3>Precisa de atendimento?</h3>
            <p>Registre o problema, a categoria e quem foi afetado.</p>
            <button onClick={() => setCreating(true)}>
              Solicitar suporte <ArrowUpRight size={16} />
            </button>
          </div>
          <div className="sidebar-footer">
            <span className="avatar">{user.nome.slice(0, 1)}</span>
            <div>
              {user.nome}
              <small>Central de suporte</small>
            </div>
            <ShieldCheck size={18} />
          </div>
        </aside>
      )}
      {!user && (
        <aside className="auth-story">
          <a className="brand" href="#">
            <span className="brand-icon">
              <Leaf size={24} />
            </span>
            Núcleo
          </a>
          <div>
            <span className="story-label">NÚCLEO / CENTRAL INTERNA</span>
            <h1>
              Problema registrado.
              <br />
              Próximo passo, solução.
            </h1>
            <p>Um lugar para pedir ajuda, acompanhar soluções e seguir em frente.</p>
            <div className="story-card">
              <span>
                <CircleCheck size={22} /> Tudo no lugar.
              </span>
              <p>Do primeiro contato à solução, cada chamado tem seu espaço.</p>
            </div>
          </div>
          <small>Feito para conectar pessoas e soluções.</small>
        </aside>
      )}
      <main className={user ? 'main-content' : 'auth-content'}>
        <header className="topbar">
          <span>
            {user ? (
              <>
                Workspace <span className="breadcrumb">/</span> Central de suporte
              </>
            ) : (
              'BEM-VINDO À SUA CENTRAL'
            )}
          </span>
          {user ? (
            <button
              className="text-button"
              disabled={busy}
              onClick={() => {
                setBusy(true);
                void api('/auth/logout', 'POST')
                  .then(() => {
                    setUser(null);
                    setTickets([]);
                    setNotice('');
                    setError('');
                  })
                  .catch((e) => setError(String(e)))
                  .finally(() => setBusy(false));
              }}
            >
              <LogOut size={16} /> Sair
            </button>
          ) : (
            <span className="secure">
              <ShieldCheck size={16} /> Acesso seguro
            </span>
          )}
        </header>
        {error && (
          <p role="alert" className="error">
            {error}
          </p>
        )}
        {notice && (
          <p role="status" className="notice">
            <CircleCheck size={18} />
            {notice}
            <button className="text-button" aria-label="Fechar aviso" onClick={() => setNotice('')}>
              <X size={16} />
            </button>
          </p>
        )}
        {!user ? (
          <section className="login">
            <span className="section-label">ACESSO À CENTRAL</span>
            <h2>Entre na sua conta.</h2>
            <p className="muted">Acesse seus chamados com o e-mail da empresa.</p>
            <form
              onSubmit={(e) =>
                void submit(e, async (data) => {
                  await api(
                    '/auth/login',
                    'POST',
                    new URLSearchParams({
                      username: String(data.get('email')),
                      password: String(data.get('senha')),
                    }),
                  );
                  const current = await api<User>('/auth/me');
                  setUser(current);
                  if (!current.trocaSenhaObrigatoria) await load();
                })
              }
            >
              <label>
                E-mail
                <input name="email" type="email" autoComplete="username" required />
              </label>
              <label>
                Senha
                <input name="senha" type="password" autoComplete="current-password" required />
              </label>
              <button disabled={busy}>
                {busy ? 'Entrando…' : 'Acessar minha central'}
                <ArrowUpRight size={18} />
              </button>
            </form>
          </section>
        ) : user.trocaSenhaObrigatoria ? (
          <section className="login">
            <h2>Defina sua nova senha</h2>
            <p>
              Escolha uma senha diferente da atual, com pelo menos 12 caracteres (máximo de 72 bytes
              UTF-8).
            </p>
            <form
              onSubmit={(e) =>
                void submit(e, async (data) => {
                  if (data.get('atual') === data.get('nova'))
                    throw new Error('Escolha uma senha diferente da atual.');
                  if (new TextEncoder().encode(String(data.get('nova'))).length > 72)
                    throw new Error('A nova senha deve ter no máximo 72 bytes UTF-8.');
                  await api('/auth/senha', 'PATCH', {
                    senhaAtual: data.get('atual'),
                    novaSenha: data.get('nova'),
                  });
                  setUser(await api<User>('/auth/me'));
                  await load();
                })
              }
            >
              <label>
                Senha atual
                <input name="atual" type="password" autoComplete="current-password" required />
              </label>
              <label>
                Nova senha
                <input
                  name="nova"
                  type="password"
                  minLength={12}
                  maxLength={72}
                  autoComplete="new-password"
                  required
                />
              </label>
              <button disabled={busy}>Salvar senha</button>
            </form>
          </section>
        ) : administration && user.perfil === 'ADMIN' ? (
          <AdminPanel onError={setError} />
        ) : (
          <>
            <div className="page-heading">
              <div>
                <span className="section-label">OPERAÇÃO / SUPORTE INTERNO</span>
                <h1>
                  {creating ? 'Como podemos ajudar?' : 'Fila de chamados'}
                  <span className="green-dot">.</span>
                </h1>
                <p className="muted">
                  {creating
                    ? 'Conte o que aconteceu. Vamos encontrar uma solução juntos.'
                    : `Olá, ${user.nome.split(' ')[0]}. Consulte solicitações e acompanhe o atendimento.`}
                </p>
              </div>
              <button
                className={creating ? 'secondary' : ''}
                onClick={() => setCreating(!creating)}
              >
                {creating ? (
                  <>
                    <ChevronLeft size={18} /> Voltar aos chamados
                  </>
                ) : (
                  <>
                    <Plus size={18} /> Novo chamado
                  </>
                )}
              </button>
            </div>
            {!creating ? (
              <>
                <div className="stats">
                  <div className="stat">
                    <span className="stat-icon mint">
                      <Layers size={21} />
                    </span>
                    <span>Total de chamados</span>
                    <strong>{dashboard?.total ?? '—'}</strong>
                    <small>Todos os seus atendimentos</small>
                  </div>
                  <div className="stat">
                    <span className="stat-icon amber">
                      <Ticket size={21} />
                    </span>
                    <span>Em aberto</span>
                    <strong>{dashboard?.porStatus.ABERTO ?? '—'}</strong>
                    <small>Aguardando início do atendimento</small>
                  </div>
                  <div className="stat">
                    <span className="stat-icon blue">
                      <Clock3 size={21} />
                    </span>
                    <span>Em atendimento</span>
                    <strong>{dashboard?.porStatus.EM_ATENDIMENTO ?? '—'}</strong>
                    <small>Com atendimento iniciado</small>
                  </div>
                  <div className="stat">
                    <span className="stat-icon green">
                      <CircleCheck size={21} />
                    </span>
                    <span>Resolvidos</span>
                    <strong>
                      {dashboard
                        ? (dashboard.porStatus.RESOLVIDO ?? 0) + (dashboard.porStatus.FECHADO ?? 0)
                        : '—'}
                    </strong>
                    <small>Resolvidos e fechados</small>
                  </div>
                </div>
                <section className="ticket-panel">
                  <div className="panel-heading">
                    <div>
                      <h2>Seus chamados</h2>
                      <p className="muted">Abra um registro para consultar os detalhes.</p>
                    </div>
                    <span className="live-label">
                      <span /> Acompanhamento
                    </span>
                  </div>
                  <form
                    className="filters"
                    onSubmit={(e) => {
                      e.preventDefault();
                      void refresh();
                    }}
                  >
                    <div className="search-field">
                      <Search size={18} />
                      <input
                        aria-label="Buscar chamados"
                        placeholder="Buscar por título ou protocolo…"
                        value={search}
                        onChange={(e) => setSearch(e.target.value)}
                      />
                    </div>
                    <select
                      aria-label="Filtrar por status"
                      value={status}
                      onChange={(e) => setStatus(e.target.value)}
                    >
                      <option value="">Todos os status</option>
                      {Object.entries(statuses).map(([key, value]) => (
                        <option key={key} value={key}>
                          {value}
                        </option>
                      ))}
                    </select>
                    <select aria-label="Filtrar por prioridade" value={priorityFilter} onChange={(e) => setPriorityFilter(e.target.value)}>
                      <option value="">Todas as prioridades</option>
                      <option value="BAIXA">Baixa</option><option value="MEDIA">Média</option><option value="ALTA">Alta</option><option value="CRITICA">Crítica</option>
                    </select>
                    <select aria-label="Filtrar por categoria" value={categoryFilter} onChange={(e) => setCategoryFilter(e.target.value)}>
                      <option value="">Todas as categorias</option>{categories.map((category) => <option key={category.id} value={category.id}>{category.nome}</option>)}
                    </select>
                    <label className="date-filter">De<input aria-label="Data inicial" type="date" value={openedFrom} onChange={(e) => setOpenedFrom(e.target.value)} /></label>
                    <label className="date-filter">Até<input aria-label="Data final" type="date" value={openedTo} onChange={(e) => setOpenedTo(e.target.value)} /></label>
                    {user.perfil !== 'SOLICITANTE' && <>
                      <select aria-label="Filtrar por responsável" value={responsibleFilter} onChange={(e) => setResponsibleFilter(e.target.value)}>
                        <option value="">Todos os responsáveis</option>{agents.map((agent) => <option key={agent.id} value={agent.id}>{agent.nome}</option>)}
                      </select>
                      <label className="date-filter">ID do setor<input aria-label="ID do setor" type="number" min="1" value={sectorFilter} onChange={(e) => setSectorFilter(e.target.value)} /></label>
                    </>}
                    <button className="secondary" disabled={busy}>
                      Buscar
                    </button>
                    <button type="button" className="secondary" disabled={busy} onClick={() => {
                      setSearch(''); setStatus(''); setPriorityFilter(''); setCategoryFilter('');
                      setOpenedFrom(''); setOpenedTo(''); setResponsibleFilter(''); setSectorFilter('');
                      void load(0, '', '', {});
                    }}>Limpar filtros</button>
                  </form>
                  <div className="list-heading">
                    <span>CHAMADO</span>
                    <span>STATUS E PRIORIDADE</span>
                  </div>
                  {busy && (
                    <p className="muted" role="status">
                      Atualizando chamados…
                    </p>
                  )}
                  {!busy && tickets.length === 0 && (
                    <div className="empty">
                      <span className="empty-icon">
                        <Ticket size={30} />
                      </span>
                      <h3>
                        {search || status || priorityFilter || categoryFilter || openedFrom || openedTo || responsibleFilter || sectorFilter
                          ? 'Nenhum resultado por aqui'
                          : 'Nenhum chamado registrado'}
                      </h3>
                      <p>
                        {search || status || priorityFilter || categoryFilter || openedFrom || openedTo || responsibleFilter || sectorFilter
                          ? 'Tente outra busca ou selecione um status diferente.'
                          : 'Precisa de uma mão? Abra seu primeiro chamado e acompanhe a solução neste espaço.'}
                      </p>
                      <button
                        className="secondary"
                        onClick={() => {
                          if (search || status || priorityFilter || categoryFilter || openedFrom || openedTo || responsibleFilter || sectorFilter) {
                            setSearch('');
                            setStatus('');
                            setPriorityFilter(''); setCategoryFilter(''); setOpenedFrom(''); setOpenedTo('');
                            setResponsibleFilter(''); setSectorFilter('');
                            void load(0, '', '', {});
                          } else setCreating(true);
                        }}
                      >
                        {search || status ? 'Limpar filtros' : 'Abrir meu primeiro chamado'}
                        <ArrowUpRight size={16} />
                      </button>
                    </div>
                  )}
                  {tickets.map((t) => (
                    <article className="ticket-row" key={t.id}>
                      <button className="ticket-summary" onClick={() => void openTicket(t.id)}>
                        <div>
                          <span className="protocol">{t.protocolo}</span>
                          <strong>{t.titulo}</strong>
                          <span className="ticket-meta">
                            {t.categoria.nome} <span>·</span>{' '}
                            {new Date(t.criadoEm).toLocaleDateString('pt-BR')}
                          </span>
                        </div>
                        <div className="ticket-badges">
                          <span className={`badge status-${t.status}`}>
                            {statuses[t.status] || t.status}
                          </span>
                          <span className={`priority priority-${t.prioridade}`}>
                            {t.prioridade.toLowerCase()}
                          </span>
                          <ChevronRight size={16} />
                        </div>
                      </button>
                    </article>
                  ))}
                  <nav className="pagination" aria-label="Páginas de chamados">
                    <span>
                      Página {page + 1} de {Math.max(1, pages)}
                    </span>
                    <div>
                      <button
                        className="secondary"
                        disabled={busy || page === 0}
                        onClick={() => {
                          void refresh(page - 1);
                        }}
                      >
                        <ChevronLeft size={16} />
                        Anterior
                      </button>
                      <button
                        className="secondary"
                        disabled={busy || page + 1 >= pages}
                        onClick={() => {
                          void refresh(page + 1);
                        }}
                      >
                        Próxima
                        <ChevronRight size={16} />
                      </button>
                    </div>
                  </nav>
                </section>
                {(detailLoading || selected || detailError) && (
                  <section className="ticket-panel detail-panel" aria-label="Detalhe do chamado">
                    <div className="panel-heading">
                      <div>
                        <span className="section-label">DETALHES E ATENDIMENTO</span>
                        <h2>{selected ? `${selected.protocolo} · ${selected.titulo}` : 'Carregando chamado…'}</h2>
                      </div>
                      <button className="secondary" onClick={() => setSelected(null)}>Fechar</button>
                    </div>
                    {detailError && <p role="alert" className="error">{detailError}</p>}
                    {selected && <>
                      <p className="description">{selected.descricao}</p>
                      <p className="muted">Solicitante: {selected.solicitante?.nome ?? '—'} · Setor: {selected.setorOrigem?.nome ?? '—'} · Responsável: {selected.responsavel?.nome ?? 'Sem responsável'}</p>
                      {(user.perfil === 'ADMIN' || selected.responsavel?.id === user.id) && <div className="detail-actions">
                        {selected.status === 'ABERTO' && !selected.responsavel && <button disabled={busy} onClick={() => void mutateTicket('/assumir', 'POST', {})}>Assumir chamado</button>}
                        {user.perfil === 'ADMIN' && <form onSubmit={(e) => { e.preventDefault(); const data = new FormData(e.currentTarget); void mutateTicket('/responsavel', 'PATCH', { responsavelId: Number(data.get('responsavelId')) }); }}>
                          <select name="responsavelId" defaultValue={selected.responsavel?.id ?? ''} required><option value="">Atribuir responsável</option>{agents.map((a) => <option key={a.id} value={a.id}>{a.nome}</option>)}</select><button disabled={busy}>Atribuir</button>
                        </form>}
                        <form onSubmit={(e) => { e.preventDefault(); const data = new FormData(e.currentTarget); void mutateTicket('/classificacao', 'PATCH', { prioridade: data.get('prioridade'), categoriaId: data.get('categoriaId') ? Number(data.get('categoriaId')) : null }); }}>
                          <select name="prioridade" defaultValue={selected.prioridade}><option value="BAIXA">Baixa</option><option value="MEDIA">Média</option><option value="ALTA">Alta</option><option value="CRITICA">Crítica</option></select>
                          <select name="categoriaId" defaultValue={selected.categoria.id}><option value={selected.categoria.id}>{selected.categoria.nome} (atual)</option>{categories.filter((category) => category.id !== selected.categoria.id).map((category) => <option key={category.id} value={category.id}>{category.nome}</option>)}</select>
                          <button disabled={busy}>Salvar classificação</button>
                        </form>
                        {selected.status === 'EM_ATENDIMENTO' && <>
                          <form onSubmit={(e) => { e.preventDefault(); const data = new FormData(e.currentTarget); void mutateTicket('/transicoes', 'POST', { destino: 'AGUARDANDO_USUARIO', mensagemPublica: data.get('mensagemPublica') }); }}><input name="mensagemPublica" minLength={10} placeholder="O que falta para continuar?" required /><button disabled={busy}>Aguardar usuário</button></form>
                          <form onSubmit={(e) => { e.preventDefault(); const data = new FormData(e.currentTarget); void mutateTicket('/transicoes', 'POST', { destino: 'RESOLVIDO', solucao: data.get('solucao') }); }}><input name="solucao" minLength={10} placeholder="Resumo da solução" required /><button disabled={busy}>Resolver</button></form>
                        </>}
                        {selected.status === 'AGUARDANDO_USUARIO' && <button disabled={busy} onClick={() => void mutateTicket('/transicoes', 'POST', { destino: 'EM_ATENDIMENTO' })}>Retomar atendimento</button>}
                      </div>}
                      {selected.status === 'RESOLVIDO' && (user.perfil === 'ADMIN' || user.id === selected.solicitante?.id) && <div className="detail-actions">
                        <button disabled={busy} onClick={() => void mutateTicket('/transicoes', 'POST', { destino: 'FECHADO' })}>Confirmar e fechar</button>
                        <form onSubmit={(e) => { e.preventDefault(); const data = new FormData(e.currentTarget); void mutateTicket('/transicoes', 'POST', { destino: 'EM_ATENDIMENTO', justificativa: data.get('justificativa') }); }}><input name="justificativa" minLength={10} placeholder="Motivo para reabrir" required /><button disabled={busy}>Solicitar reabertura</button></form>
                      </div>}
                      {(selected.status === 'ABERTO' && (user.perfil === 'ADMIN' || user.id === selected.solicitante?.id) || ['EM_ATENDIMENTO', 'AGUARDANDO_USUARIO'].includes(selected.status) && user.perfil === 'ADMIN') && <form className="detail-actions" onSubmit={(e) => { e.preventDefault(); const data = new FormData(e.currentTarget); void mutateTicket('/transicoes', 'POST', { destino: 'CANCELADO', justificativa: data.get('justificativa') }); }}><input name="justificativa" minLength={10} placeholder="Motivo do cancelamento" required /><button className="secondary" disabled={busy}>Cancelar chamado</button></form>}
                      {selected.solucao && <p><strong>Solução registrada:</strong> {selected.solucao}</p>}
                      <h3>Conversa</h3>
                      <div className="conversation">{comments.map((c) => <p key={c.id}><strong>{c.autor.nome}</strong> {c.visibilidade === 'INTERNO' && <span className="badge">Nota interna</span>}<br />{c.conteudo}<small>{new Date(c.criadoEm).toLocaleString('pt-BR')}</small></p>)}</div>
                      {!['FECHADO', 'CANCELADO'].includes(selected.status) && <form className="detail-actions" onSubmit={(e) => { e.preventDefault(); const data = new FormData(e.currentTarget); void mutateTicket('/comentarios', 'POST', { conteudo: data.get('conteudo'), visibilidade: data.get('visibilidade') }); e.currentTarget.reset(); }}><textarea name="conteudo" minLength={1} maxLength={5000} placeholder="Escreva uma mensagem" required /><select name="visibilidade"><option value="PUBLICO">Mensagem pública</option>{user.perfil !== 'SOLICITANTE' && <option value="INTERNO">Nota interna</option>}</select><button disabled={busy}>Enviar mensagem</button></form>}
                      <h3>Histórico</h3><ol className="history-list">{history.map((h) => <li key={h.id}><strong>{h.tipoEvento}</strong>: {h.valorAnterior ?? '—'} → {h.valorNovo ?? '—'}{h.justificativa ? ` · ${h.justificativa}` : ''}<small>{h.autor.nome} · {new Date(h.criadoEm).toLocaleString('pt-BR')}</small></li>)}</ol>
                    </>}
                  </section>
                )}
                <div className="bottom-note">
                  <Leaf size={15} /> Núcleo / Central interna de suporte
                </div>
              </>
            ) : (
              <div className="create-layout">
                <section className="create-panel">
                  <div className="form-title">
                    <span className="stat-icon mint">
                      <Ticket size={22} />
                    </span>
                    <div>
                      <h2>Nova solicitação</h2>
                      <p className="muted">Preencha os dados para iniciar o atendimento.</p>
                    </div>
                  </div>
                  <form
                    onSubmit={(e) =>
                      void submit(e, async (data) => {
                        await api('/chamados', 'POST', {
                          titulo: data.get('titulo'),
                          descricao: data.get('descricao'),
                          categoriaId: Number(data.get('categoria')),
                          impacto: data.get('impacto'),
                        });
                        setSearch('');
                        setStatus('');
                        await load();
                        setCreating(false);
                        setNotice('Chamado enviado! Você já pode acompanhar o andamento abaixo.');
                      })
                    }
                  >
                    <label>
                      O que está acontecendo?
                      <input
                        name="titulo"
                        placeholder="Ex.: Não consigo acessar meu e-mail"
                        minLength={5}
                        maxLength={150}
                        required
                      />
                    </label>
                    <label>
                      Conte um pouco mais
                      <textarea
                        name="descricao"
                        placeholder="Descreva o problema, quando começou e o que você já tentou fazer…"
                        minLength={10}
                        maxLength={5000}
                        rows={6}
                        required
                      />
                      <small className="muted">
                        Inclua os detalhes que podem ajudar a equipe. Mínimo de 10 caracteres.
                      </small>
                    </label>
                    <div className="form-grid">
                      <label>
                        Categoria
                        <select name="categoria" required>
                          <option value="">Selecione uma categoria</option>
                          {categories.map((c) => (
                            <option value={c.id} key={c.id}>
                              {c.nome}
                            </option>
                          ))}
                        </select>
                      </label>
                      <label>
                        Quem foi afetado?
                        <select name="impacto">
                          <option value="INDIVIDUAL">Somente eu</option>
                          <option value="SETOR">Meu setor</option>
                          <option value="ORGANIZACAO">Toda a organização</option>
                        </select>
                      </label>
                    </div>
                    {categories.length === 0 && (
                      <p className="error">Não há categorias ativas cadastradas.</p>
                    )}
                    <div className="form-actions">
                      <button
                        type="button"
                        className="secondary"
                        onClick={() => setCreating(false)}
                      >
                        Cancelar
                      </button>
                      <button disabled={busy || categories.length === 0}>
                        <Send size={17} />
                        {busy ? 'Enviando…' : 'Enviar chamado'}
                      </button>
                    </div>
                  </form>
                </section>
                <aside className="tips">
                  <span className="section-label">ORIENTAÇÕES</span>
                  <h2>Antes de enviar.</h2>
                  <p>Uma descrição clara ajuda nossa equipe a chegar mais rápido à solução.</p>
                  <ol>
                    <li>Explique o que você precisava fazer.</li>
                    <li>Descreva a mensagem de erro, se houver.</li>
                    <li>Conte se mais alguém foi afetado.</li>
                  </ol>
                  <div>
                    <ShieldCheck size={20} />
                    <p>Evite incluir senhas ou informações sensíveis na descrição.</p>
                  </div>
                </aside>
              </div>
            )}
          </>
        )}
      </main>
    </div>
  );
}
createRoot(document.getElementById('root')!).render(<App />);
