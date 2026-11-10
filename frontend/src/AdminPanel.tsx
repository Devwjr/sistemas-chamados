import { useCallback, useEffect, useState } from 'react';
import type { FormEvent } from 'react';

type Ref = { id: number; nome: string };
type Cadastro = { id: number; nome: string; descricao: string | null; ativo: boolean };
type Usuario = { id: number; nome: string; email: string; perfil: string; setor: Ref; ativo: boolean };
type Page<T> = { items: T[] };
type Tab = 'usuarios' | 'setores' | 'categorias';

async function request<T>(path: string, method = 'GET', body?: object): Promise<T> {
  const headers = new Headers();
  if (method !== 'GET') {
    const csrfResponse = await fetch('/api/auth/csrf');
    const csrf = (await csrfResponse.json()) as { headerName: string; token: string };
    headers.set(csrf.headerName, csrf.token);
    headers.set('Content-Type', 'application/json');
  }
  const response = await fetch(`/api${path}`, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined,
  });
  if (!response.ok) {
    const error = (await response.json().catch(() => ({}))) as { detail?: string; message?: string };
    throw new Error(error.detail || error.message || `Falha na solicitação (${response.status}).`);
  }
  return response.status === 204 ? (undefined as T) : response.json();
}

export function AdminPanel({ onError }: { onError: (message: string) => void }) {
  const [tab, setTab] = useState<Tab>('usuarios');
  const [users, setUsers] = useState<Usuario[]>([]);
  const [sectors, setSectors] = useState<Cadastro[]>([]);
  const [categories, setCategories] = useState<Cadastro[]>([]);
  const [editing, setEditing] = useState<Usuario | Cadastro | null>(null);
  const [busy, setBusy] = useState(false);

  const refresh = useCallback(async () => {
    try {
      const [u, s, c] = await Promise.all([
        request<Page<Usuario>>('/usuarios?size=100'),
        request<Page<Cadastro>>('/setores?size=100'),
        request<Page<Cadastro>>('/categorias?size=100'),
      ]);
      setUsers(u.items);
      setSectors(s.items);
      setCategories(c.items);
    } catch (error) {
      onError(error instanceof Error ? error.message : 'Não foi possível carregar os cadastros.');
    }
  }, [onError]);
  useEffect(() => { void refresh(); }, [refresh]);

  const rows = tab === 'usuarios' ? users : tab === 'setores' ? sectors : categories;
  const basePath = tab === 'usuarios' ? '/usuarios' : tab === 'setores' ? '/setores' : '/categorias';
  function beginCreate() {
    setEditing(tab === 'usuarios'
      ? { id: 0, nome: '', email: '', perfil: 'SOLICITANTE', setor: { id: 0, nome: '' }, ativo: true }
      : { id: 0, nome: '', descricao: '', ativo: true });
  }
  async function save(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!editing) return;
    const data = new FormData(event.currentTarget);
    const body = tab === 'usuarios'
      ? {
          nome: data.get('nome'), email: data.get('email'), perfil: data.get('perfil'),
          setorId: Number(data.get('setorId')), ativo: data.get('ativo') === 'true',
          ...(!editing.id ? { senhaTemporaria: data.get('senhaTemporaria') } : {}),
        }
      : { nome: data.get('nome'), descricao: data.get('descricao') || null, ativo: data.get('ativo') === 'true' };
    setBusy(true);
    try {
      const path = editing.id ? `${basePath}/${editing.id}` : basePath;
      await request(path, editing.id ? 'PATCH' : 'POST', body);
      setEditing(null);
      await refresh();
    } catch (error) {
      onError(error instanceof Error ? error.message : 'Não foi possível salvar o cadastro.');
    } finally {
      setBusy(false);
    }
  }
  async function resetPassword(user: Usuario) {
    const senhaTemporaria = window.prompt(`Nova senha temporária para ${user.nome} (mínimo 12 caracteres):`);
    if (!senhaTemporaria) return;
    setBusy(true);
    try {
      await request(`/usuarios/${user.id}/redefinir-senha`, 'POST', { senhaTemporaria });
    } catch (error) {
      onError(error instanceof Error ? error.message : 'Não foi possível redefinir a senha.');
    } finally {
      setBusy(false);
    }
  }

  return <section className="ticket-panel admin-panel">
    <div className="panel-heading"><div><span className="section-label">ADMINISTRAÇÃO</span><h2>Cadastros</h2></div><button onClick={beginCreate}>Novo cadastro</button></div>
    <div className="filters admin-tabs">{(['usuarios', 'setores', 'categorias'] as Tab[]).map((name) => <button key={name} className={tab === name ? '' : 'secondary'} onClick={() => { setTab(name); setEditing(null); }}>{name[0].toUpperCase() + name.slice(1)}</button>)}</div>
    {editing && <form className="admin-form" onSubmit={(event) => void save(event)}>
      <h3>{editing.id ? 'Editar cadastro' : 'Novo cadastro'}</h3>
      <label>Nome<input name="nome" defaultValue={editing.nome} minLength={2} maxLength={tab === 'usuarios' ? 120 : 100} required /></label>
      {tab === 'usuarios' ? <>
        <label>E-mail<input name="email" type="email" defaultValue={'email' in editing ? editing.email : ''} maxLength={200} required /></label>
        <label>Perfil<select name="perfil" defaultValue={'perfil' in editing ? editing.perfil : 'SOLICITANTE'}><option value="SOLICITANTE">Solicitante</option><option value="TECNICO">Técnico</option><option value="ADMIN">Administrador</option></select></label>
        <label>Setor<select name="setorId" defaultValue={'setor' in editing ? editing.setor.id : ''} required><option value="">Selecione</option>{sectors.filter((s) => s.ativo).map((s) => <option key={s.id} value={s.id}>{s.nome}</option>)}</select></label>
        {!editing.id && <label>Senha temporária<input name="senhaTemporaria" type="password" minLength={12} maxLength={72} required /></label>}
      </> : <label>Descrição<textarea name="descricao" defaultValue={'descricao' in editing ? editing.descricao ?? '' : ''} maxLength={500} /></label>}
      <label>Status<select name="ativo" defaultValue={editing.ativo ? 'true' : 'false'}><option value="true">Ativo</option><option value="false">Inativo</option></select></label>
      <div className="form-actions"><button type="button" className="secondary" onClick={() => setEditing(null)}>Cancelar</button><button disabled={busy}>Salvar</button></div>
    </form>}
    <div className="admin-list">{rows.map((row) => <article key={row.id} className="admin-row"><div><strong>{row.nome}</strong><small>{tab === 'usuarios' && 'email' in row ? `${row.email} · ${row.perfil} · ${row.setor.nome}` : row.ativo ? 'Ativo' : 'Inativo'}</small></div><div><button className="secondary" onClick={() => setEditing(row)}>Editar</button>{tab === 'usuarios' && 'email' in row && <button className="secondary" disabled={busy} onClick={() => void resetPassword(row)}>Redefinir senha</button>}</div></article>)}</div>
  </section>;
}
