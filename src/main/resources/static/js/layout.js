// Cabeçalho comum, sessão do usuário e utilitários de interface.
import { api, esc } from './api.js';
import { carrinho } from './carrinho.js';

let usuarioAtual;

export async function usuarioLogado() {
  if (usuarioAtual === undefined) {
    try {
      usuarioAtual = await api.get('/api/auth/me');
    } catch {
      usuarioAtual = null;
    }
  }
  return usuarioAtual;
}

/** Redireciona para o login se não houver sessão; opcionalmente exige ADMIN. */
export async function exigirLogin({ admin = false } = {}) {
  const usuario = await usuarioLogado();
  if (!usuario) {
    const voltar = location.pathname.replace(/^\//, '') + location.search;
    location.href = 'login.html?voltar=' + encodeURIComponent(voltar);
    return new Promise(() => {});
  }
  if (admin && usuario.papel !== 'ADMIN') {
    document.querySelector('main').innerHTML =
      '<div class="alerta alerta-erro" role="alert">Acesso restrito ao administrador.</div>';
    return new Promise(() => {});
  }
  return usuario;
}

function atualizarContador() {
  const el = document.getElementById('contador-carrinho');
  if (el) el.textContent = carrinho.quantidadeTotal();
}

async function montarCabecalho() {
  const topo = document.getElementById('topo');
  if (!topo) return;
  const usuario = await usuarioLogado();
  const links = ['<a href="index.html">Vitrine</a>'];
  if (usuario) {
    links.push('<a href="meus-pedidos.html">Meus pedidos</a>');
    if (usuario.papel === 'ADMIN') links.push('<a href="admin.html">Administração</a>');
    links.push(`<span class="usuario">Olá, ${esc(usuario.nome.split(' ')[0])}</span>`);
    links.push('<button type="button" class="link" id="btn-sair">Sair</button>');
  } else {
    links.push('<a href="login.html">Entrar</a>');
  }
  links.push('<a class="carrinho-link" href="carrinho.html">Carrinho (<span id="contador-carrinho">0</span>)</a>');

  topo.innerHTML = `
    <div class="aviso-demo">Ambiente de demonstração acadêmica — pagamentos PIX são simulados.</div>
    <div class="container">
      <a class="marca" href="index.html"><img src="favicon.svg" alt="">Cupcakes Gourmet</a>
      <nav class="nav" aria-label="Principal">${links.join('')}</nav>
    </div>`;
  atualizarContador();

  const sair = document.getElementById('btn-sair');
  if (sair) {
    sair.addEventListener('click', async () => {
      try {
        await api.post('/api/auth/logout');
      } finally {
        location.href = 'index.html';
      }
    });
  }
}

document.addEventListener('carrinho:alterado', atualizarContador);
window.addEventListener('storage', atualizarContador);
montarCabecalho();

let toastTimer;
export function toast(mensagem, tipo = 'ok') {
  let el = document.getElementById('toast');
  if (!el) {
    el = document.createElement('div');
    el.id = 'toast';
    el.setAttribute('role', 'status');
    el.setAttribute('aria-live', 'polite');
    document.body.appendChild(el);
  }
  el.className = 'toast' + (tipo === 'erro' ? ' erro' : '');
  el.textContent = mensagem;
  el.hidden = false;
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => { el.hidden = true; }, 3500);
}

/** Mostra um alerta dentro do elemento (ou o limpa quando mensagem é vazia). */
export function alerta(el, mensagem, tipo = 'erro') {
  if (!mensagem) {
    el.hidden = true;
    el.textContent = '';
    return;
  }
  el.className = `alerta alerta-${tipo}`;
  el.setAttribute('role', tipo === 'erro' ? 'alert' : 'status');
  el.textContent = mensagem;
  el.hidden = false;
  el.scrollIntoView({ block: 'nearest', behavior: 'smooth' });
}

/** Marca os campos inválidos do formulário com as mensagens do ProblemDetail (MSG03). */
export function mostrarErrosDeCampo(form, erros = {}) {
  form.querySelectorAll('.campo').forEach((c) => {
    c.classList.remove('invalido');
    const msg = c.querySelector('.erro-campo');
    if (msg) msg.remove();
  });
  Object.entries(erros).forEach(([nome, mensagem]) => {
    const input = form.elements[nome];
    if (!input || !input.closest) return;
    const campo = input.closest('.campo');
    if (!campo) return;
    campo.classList.add('invalido');
    const span = document.createElement('span');
    span.className = 'erro-campo';
    span.id = `erro-${nome}`;
    span.textContent = mensagem;
    input.setAttribute('aria-describedby', span.id);
    campo.appendChild(span);
  });
}

/** Desabilita o botão enquanto a ação roda (evita clique duplo). */
export async function comBotaoOcupado(botao, texto, acao) {
  const original = botao.textContent;
  botao.disabled = true;
  botao.textContent = texto;
  try {
    return await acao();
  } finally {
    botao.disabled = false;
    botao.textContent = original;
  }
}
