import { describe, expect, it } from 'vitest';
import { buildTicketQuery } from './ticketFilters';

describe('buildTicketQuery', () => {
  it('inclui os filtros informados e omite os vazios', () => {
    const params = buildTicketQuery(2, 'CH-000041', 'ABERTO', {
      prioridade: 'ALTA',
      categoriaId: '3',
      de: '2026-01-01',
      ate: '',
      responsavelId: '9',
      setorId: '',
    });

    expect(Object.fromEntries(params.entries())).toEqual({
      page: '2',
      busca: 'CH-000041',
      status: 'ABERTO',
      prioridade: 'ALTA',
      categoriaId: '3',
      de: '2026-01-01',
      responsavelId: '9',
    });
  });

  it('mantém busca e paginação sem filtros adicionais', () => {
    const params = buildTicketQuery(0, 'acesso à rede', '');

    expect(params.get('page')).toBe('0');
    expect(params.get('busca')).toBe('acesso à rede');
    expect([...params.keys()]).toEqual(['page', 'busca']);
  });
});
