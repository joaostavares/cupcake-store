// Cliente HTTP da API REST: JSON, cookie de sessão e token CSRF (cookie XSRF-TOKEN -> cabeçalho X-XSRF-TOKEN).

export class ApiError extends Error {
  constructor(status, mensagem, problema) {
    super(mensagem);
    this.status = status;
    this.problema = problema || {};
  }
  get erros() {
    return this.problema.erros || {};
  }
}

function lerCookie(nome) {
  const m = document.cookie.match(new RegExp('(?:^|; )' + nome + '=([^;]*)'));
  return m ? decodeURIComponent(m[1]) : null;
}

async function garantirCsrf() {
  if (!lerCookie('XSRF-TOKEN')) {
    await fetch('/api/auth/me', { credentials: 'same-origin' });
  }
  return lerCookie('XSRF-TOKEN');
}

async function requisicao(metodo, url, corpo) {
  const headers = { Accept: 'application/json' };
  if (corpo !== undefined) headers['Content-Type'] = 'application/json';
  if (metodo !== 'GET') {
    const token = await garantirCsrf();
    if (token) headers['X-XSRF-TOKEN'] = token;
  }
  let resp;
  try {
    resp = await fetch(url, {
      method: metodo,
      headers,
      credentials: 'same-origin',
      body: corpo !== undefined ? JSON.stringify(corpo) : undefined,
    });
  } catch (e) {
    throw new ApiError(0, 'Não foi possível falar com o servidor. Verifique sua conexão e tente novamente.');
  }
  if (resp.status === 204) return null;
  const tipo = resp.headers.get('content-type') || '';
  const dados = tipo.includes('json') ? await resp.json() : null;
  if (!resp.ok) {
    const msg = (dados && dados.detail) || 'Ocorreu um erro inesperado. Tente novamente em instantes.';
    throw new ApiError(resp.status, msg, dados);
  }
  return dados;
}

export const api = {
  get: (url) => requisicao('GET', url),
  post: (url, corpo) => requisicao('POST', url, corpo),
  put: (url, corpo) => requisicao('PUT', url, corpo),
  patch: (url, corpo) => requisicao('PATCH', url, corpo),
};

const moeda = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
const dataHora = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short' });

export const formatar = {
  moeda: (v) => moeda.format(Number(v || 0)),
  dataHora: (iso) => (iso ? dataHora.format(new Date(iso)) : '—'),
};

export const STATUS_PEDIDO = {
  AGUARDANDO_PAGAMENTO: 'Aguardando pagamento',
  PAGO: 'Pago',
  EM_PREPARO: 'Em preparo',
  ENVIADO: 'Enviado',
  ENTREGUE: 'Entregue',
  CANCELADO: 'Cancelado',
};

export const STATUS_PAGAMENTO = { PENDENTE: 'Pendente', CONFIRMADO: 'Confirmado', CANCELADO: 'Cancelado' };

/** Escapa texto antes de interpolar em HTML. */
export function esc(valor) {
  return String(valor ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}

export function selo(status, rotulos = STATUS_PEDIDO) {
  return `<span class="status status-${esc(status)}">${esc(rotulos[status] || status)}</span>`;
}

export function imagem(url) {
  return esc(url || '/img/cupcake-padrao.svg');
}
