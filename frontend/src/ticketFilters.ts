export type TicketFilters = {
  prioridade?: string;
  categoriaId?: string;
  de?: string;
  ate?: string;
  responsavelId?: string;
  setorId?: string;
};

export function buildTicketQuery(
  page: number,
  search: string,
  status: string,
  filters: TicketFilters = {},
): URLSearchParams {
  const params = new URLSearchParams({ page: String(page), busca: search });
  if (status) params.set('status', status);
  for (const [key, value] of Object.entries(filters)) {
    if (value) params.set(key, value);
  }
  return params;
}
