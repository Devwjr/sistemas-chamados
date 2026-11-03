import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { createRoot } from 'react-dom/client';
import { Headphones, LayoutDashboard, Plus, Search, ArrowUpRight, LogOut, Ticket, Clock3, CircleCheck, Layers, Leaf, ChevronLeft, ChevronRight, X, Send, ShieldCheck } from 'lucide-react';
import '@fontsource/dm-sans/latin-400.css';
import '@fontsource/dm-sans/latin-500.css';
import '@fontsource/dm-sans/latin-600.css';
import '@fontsource/manrope/latin-600.css';
import '@fontsource/manrope/latin-700.css';
import '@fontsource/manrope/latin-800.css';
import './style.css';
type User = { nome: string; trocaSenhaObrigatoria: boolean };
type Ref = { id: number; nome: string };
type Dashboard = { total: number; porStatus: Record<string, number>; semResponsavel: number };
const statuses: Record<string, string> = { ABERTO: 'Aberto', EM_ATENDIMENTO: 'Em atendimento', AGUARDANDO_USUARIO: 'Aguardando resposta', RESOLVIDO: 'Resolvido', FECHADO: 'Fechado', CANCELADO: 'Cancelado' };
type Ticket = { id: number; protocolo: string; titulo: string; descricao: string; status: string; prioridade: string; categoria: Ref; responsavel: Ref | null; criadoEm: string };
type Page<T> = { items: T[]; totalPages: number };
async function api<T>(path: string, method = 'GET', body?: object | URLSearchParams): Promise<T> {
  const headers = new Headers();
  if (method !== 'GET') {
    const r = await fetch('/api/auth/csrf');
    if (!r.ok) throw new Error('Não foi possível preparar a sessão.');
    const csrf = await r.json() as { headerName: string; token: string };
    headers.set(csrf.headerName, csrf.token);
    headers.set('Content-Type', body instanceof URLSearchParams ? 'application/x-www-form-urlencoded' : 'application/json');
  }
  const r = await fetch('/api' + path, { method, headers, body: body instanceof URLSearchParams ? body : body ? JSON.stringify(body) : undefined });
  if (!r.ok) {
    const error = await r.json().catch(() => ({})) as { detail?: string; message?: string };
    throw new Error(error.detail || error.message || `Falha na solicitação (${r.status}).`);
  }
  return r.status === 204 ? undefined as T : r.json();
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
  const [creating, setCreating] = useState(false);
  const [notice, setNotice] = useState('');
  async function load(index = 0, query = '', filter = '') {
    const params = new URLSearchParams({ page: String(index), busca: query });
    if (filter) params.set('status', filter);
    const [list, cats, stats] = await Promise.all([api<Page<Ticket>>(`/chamados?${params}`), api<Page<Ref>>('/categorias?ativo=true&size=100'), api<Dashboard>('/painel')]);
    setDashboard(stats);
    setTickets(list.items); setPages(list.totalPages); setPage(index); setCategories(cats.items);
  }
  useEffect(() => { void api<User>('/auth/me').then(async current => {
    setUser(current); if (!current.trocaSenhaObrigatoria) await load();
  }).catch(() => setUser(null)).finally(() => setLoading(false)); }, []);
  async function submit(event: FormEvent<HTMLFormElement>, action: (data: FormData) => Promise<void>) {
    event.preventDefault(); const form = event.currentTarget; setBusy(true); setError('');
    try { await action(new FormData(form)); form.reset(); }
    catch (e) { setError(e instanceof Error ? e.message : 'Erro inesperado.'); }
    finally { setBusy(false); }
  }
  async function refresh(index = 0, query = search, filter = status) {
    setBusy(true); setError('');
    try { await load(index, query, filter); } catch (e) { setError(e instanceof Error ? e.message : 'Não foi possível carregar os chamados.'); } finally { setBusy(false); }
  }
  if (loading) return <main className="loading"><Leaf size={32} /><p>Preparando seu espaço…</p></main>;
  return <div className={user ? 'app-shell' : 'auth-shell'}>
  {user && <aside className="sidebar"><a className="brand" href="#"><span className="brand-icon"><Leaf size={23} /></span><span>Núcleo<small>SUPORTE TI</small></span></a><div className="workspace"><span className="workspace-icon"><Headphones size={18}/></span><div>Central de atendimento<small>Seu espaço de trabalho</small></div></div><span className="nav-caption">PRINCIPAL</span><nav className="side-nav"><button className={!creating ? 'active' : ''} onClick={() => setCreating(false)}><LayoutDashboard size={19}/>Visão geral<span className="nav-dot"/></button><button className={creating ? 'active' : ''} onClick={() => setCreating(true)}><Plus size={19}/>Novo chamado<ArrowUpRight size={15}/></button></nav><div className="side-help"><span className="help-icon"><Headphones size={21}/></span><h3>Conte com a gente.</h3><p>Descreva o que aconteceu e nossa equipe acompanha com você.</p><button onClick={() => setCreating(true)}>Solicitar suporte <ArrowUpRight size={16}/></button></div><div className="sidebar-footer"><span className="avatar">{user.nome.slice(0, 1)}</span><div>{user.nome}<small>Central de suporte</small></div><ShieldCheck size={18}/></div></aside>}
  {!user && <aside className="auth-story"><a className="brand" href="#"><span className="brand-icon"><Leaf size={24}/></span>Núcleo</a><div><span className="story-label">MENOS IMPREVISTOS. MAIS FLUIDEZ.</span><h1>Seu trabalho flui.<br/>O suporte cuida.</h1><p>Um lugar para pedir ajuda, acompanhar soluções e seguir em frente.</p><div className="story-card"><span><CircleCheck size={22}/> Tudo no lugar.</span><p>Do primeiro contato à solução, cada chamado tem seu espaço.</p></div></div><small>Feito para conectar pessoas e soluções.</small></aside>}
  <main className={user ? 'main-content' : 'auth-content'}>
  <header className="topbar"><span>{user ? <>Workspace <span className="breadcrumb">/</span> Central de suporte</> : 'BEM-VINDO À SUA CENTRAL'}</span>{user ? <button className="text-button" disabled={busy} onClick={() => { setBusy(true); void api('/auth/logout', 'POST').then(() => { setUser(null); setTickets([]); setNotice(''); setError(''); }).catch(e => setError(String(e))).finally(() => setBusy(false)); }}><LogOut size={16}/> Sair</button> : <span className="secure"><ShieldCheck size={16}/> Acesso seguro</span>}</header>
  {error && <p role="alert" className="error">{error}</p>}
  {notice && <p role="status" className="notice"><CircleCheck size={18}/>{notice}<button className="text-button" aria-label="Fechar aviso" onClick={() => setNotice('')}><X size={16}/></button></p>}
  {!user ? <section className="login"><span className="section-label">BOM TER VOCÊ POR AQUI</span><h2>Entre na sua conta.</h2><p className="muted">Seu próximo passo começa com uma solução.</p><form onSubmit={e => void submit(e, async data => {
    await api('/auth/login', 'POST', new URLSearchParams({ username: String(data.get('email')), password: String(data.get('senha')) }));
    const current = await api<User>('/auth/me'); setUser(current); if (!current.trocaSenhaObrigatoria) await load();
  })}><label>E-mail<input name="email" type="email" autoComplete="username" required /></label><label>Senha<input name="senha" type="password" autoComplete="current-password" required /></label><button disabled={busy}>{busy ? 'Entrando…' : 'Acessar minha central'}<ArrowUpRight size={18}/></button></form></section>
  : user.trocaSenhaObrigatoria ? <section className="login"><h2>Defina sua nova senha</h2><p>Escolha uma senha diferente da atual, com pelo menos 12 caracteres (máximo de 72 bytes UTF-8).</p><form onSubmit={e => void submit(e, async data => {
    if (data.get('atual') === data.get('nova')) throw new Error('Escolha uma senha diferente da atual.');
    if (new TextEncoder().encode(String(data.get('nova'))).length > 72) throw new Error('A nova senha deve ter no máximo 72 bytes UTF-8.');
    await api('/auth/senha', 'PATCH', { senhaAtual: data.get('atual'), novaSenha: data.get('nova') }); setUser(await api<User>('/auth/me')); await load();
  })}><label>Senha atual<input name="atual" type="password" autoComplete="current-password" required /></label><label>Nova senha<input name="nova" type="password" minLength={12} maxLength={72} autoComplete="new-password" required /></label><button disabled={busy}>Salvar senha</button></form></section>
  : <>
    <div className="page-heading"><div><span className="section-label">SEU DIA, MAIS ORGANIZADO</span><h1>{creating ? 'Como podemos ajudar?' : 'Visão geral'}<span className="green-dot">.</span></h1><p className="muted">{creating ? 'Conte o que aconteceu. Vamos encontrar uma solução juntos.' : `Olá, ${user.nome.split(' ')[0]}. Acompanhe o que está acontecendo no suporte.`}</p></div><button className={creating ? 'secondary' : ''} onClick={() => setCreating(!creating)}>{creating ? <><ChevronLeft size={18}/> Voltar aos chamados</> : <><Plus size={18}/> Novo chamado</>}</button></div>
    {!creating ? <>
    <div className="stats"><div className="stat"><span className="stat-icon mint"><Layers size={21}/></span><span>Total de chamados</span><strong>{dashboard?.total ?? '—'}</strong><small>Todos os seus atendimentos</small></div><div className="stat"><span className="stat-icon amber"><Ticket size={21}/></span><span>Em aberto</span><strong>{dashboard?.porStatus.ABERTO ?? '—'}</strong><small>Aguardando início do atendimento</small></div><div className="stat"><span className="stat-icon blue"><Clock3 size={21}/></span><span>Em atendimento</span><strong>{dashboard?.porStatus.EM_ATENDIMENTO ?? '—'}</strong><small>Nossa equipe está trabalhando</small></div><div className="stat"><span className="stat-icon green"><CircleCheck size={21}/></span><span>Resolvidos</span><strong>{dashboard ? (dashboard.porStatus.RESOLVIDO ?? 0) + (dashboard.porStatus.FECHADO ?? 0) : '—'}</strong><small>Soluções que fizeram a diferença</small></div></div>
    <section className="ticket-panel"><div className="panel-heading"><div><h2>Seus chamados</h2><p className="muted">Cada solicitação, do começo à solução.</p></div><span className="live-label"><span/> Acompanhamento</span></div><form className="filters" onSubmit={e => { e.preventDefault(); void refresh(); }}><div className="search-field"><Search size={18}/><input aria-label="Buscar chamados" placeholder="Buscar por título ou descrição…" value={search} onChange={e => setSearch(e.target.value)}/></div><select aria-label="Filtrar por status" value={status} onChange={e => { setStatus(e.target.value); void refresh(0, search, e.target.value); }}><option value="">Todos os status</option>{Object.entries(statuses).map(([key, value]) => <option key={key} value={key}>{value}</option>)}</select><button className="secondary" disabled={busy}>Buscar</button></form>
    <div className="list-heading"><span>CHAMADO</span><span>STATUS E PRIORIDADE</span></div>
    {busy && <p className="muted" role="status">Atualizando chamados…</p>}
    {!busy && tickets.length === 0 && <div className="empty"><span className="empty-icon"><Ticket size={30}/></span><h3>{search || status ? 'Nenhum resultado por aqui' : 'Tudo tranquilo por aqui'}</h3><p>{search || status ? 'Tente outra busca ou selecione um status diferente.' : 'Precisa de uma mão? Abra seu primeiro chamado e acompanhe a solução neste espaço.'}</p><button className="secondary" onClick={() => { if (search || status) { setSearch(''); setStatus(''); void refresh(0, '', ''); } else setCreating(true); }}>{search || status ? 'Limpar filtros' : 'Abrir meu primeiro chamado'}<ArrowUpRight size={16}/></button></div>}
    {tickets.map(t => <details className="ticket-row" key={t.id}><summary><div><span className="protocol">{t.protocolo}</span><strong>{t.titulo}</strong><span className="ticket-meta">{t.categoria.nome} <span>·</span> {new Date(t.criadoEm).toLocaleDateString('pt-BR')}</span></div><div className="ticket-badges"><span className={`badge status-${t.status}`}>{statuses[t.status] || t.status}</span><span className={`priority priority-${t.prioridade}`}>{t.prioridade.toLowerCase()}</span><ChevronRight size={16}/></div></summary><div className="ticket-detail"><span className="section-label">DESCRIÇÃO</span><p className="description">{t.descricao}</p><p className="muted">Responsável: {t.responsavel?.nome || 'Aguardando atribuição'}</p></div></details>)}
    <nav className="pagination" aria-label="Páginas de chamados"><span>Página {page + 1} de {Math.max(1, pages)}</span><div><button className="secondary" disabled={busy || page === 0} onClick={() => { void refresh(page - 1); }}><ChevronLeft size={16}/>Anterior</button><button className="secondary" disabled={busy || page + 1 >= pages} onClick={() => { void refresh(page + 1); }}>Próxima<ChevronRight size={16}/></button></div></nav></section>
    <div className="bottom-note"><Leaf size={15}/> Pequenos pedidos. Grandes soluções.</div>
    </> : <div className="create-layout"><section className="create-panel"><div className="form-title"><span className="stat-icon mint"><Ticket size={22}/></span><div><h2>Nova solicitação</h2><p className="muted">Quanto mais detalhes, melhor podemos ajudar.</p></div></div><form onSubmit={e => void submit(e, async data => {
    await api('/chamados', 'POST', { titulo: data.get('titulo'), descricao: data.get('descricao'), categoriaId: Number(data.get('categoria')), impacto: data.get('impacto') }); setSearch(''); setStatus(''); await load(); setCreating(false); setNotice('Chamado enviado! Você já pode acompanhar o andamento abaixo.');
  })}><label>O que está acontecendo?<input name="titulo" placeholder="Ex.: Não consigo acessar meu e-mail" minLength={5} maxLength={150} required /></label><label>Conte um pouco mais<textarea name="descricao" placeholder="Descreva o problema, quando começou e o que você já tentou fazer…" minLength={10} maxLength={5000} rows={6} required /><small className="muted">Inclua os detalhes que podem ajudar a equipe. Mínimo de 10 caracteres.</small></label><div className="form-grid"><label>Categoria<select name="categoria" required><option value="">Selecione uma categoria</option>{categories.map(c => <option value={c.id} key={c.id}>{c.nome}</option>)}</select></label><label>Quem foi afetado?<select name="impacto"><option value="INDIVIDUAL">Somente eu</option><option value="SETOR">Meu setor</option><option value="ORGANIZACAO">Toda a organização</option></select></label></div>{categories.length === 0 && <p className="error">Não há categorias ativas cadastradas.</p>}<div className="form-actions"><button type="button" className="secondary" onClick={() => setCreating(false)}>Cancelar</button><button disabled={busy || categories.length === 0}><Send size={17}/>{busy ? 'Enviando…' : 'Enviar chamado'}</button></div></form></section><aside className="tips"><span className="section-label">UM BOM COMEÇO</span><h2>Os detalhes fazem<br/>a diferença.</h2><p>Uma descrição clara ajuda nossa equipe a chegar mais rápido à solução.</p><ol><li>Explique o que você precisava fazer.</li><li>Descreva a mensagem de erro, se houver.</li><li>Conte se mais alguém foi afetado.</li></ol><div><ShieldCheck size={20}/><p>Evite incluir senhas ou informações sensíveis na descrição.</p></div></aside></div>}
  </>}
  </main></div>;

}
createRoot(document.getElementById('root')!).render(<App />);
