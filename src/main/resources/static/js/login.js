// TELA04 — Login e cadastro (CU04).
import { api } from './api.js';
import { alerta, comBotaoOcupado, mostrarErrosDeCampo } from './layout.js';

const formLogin = document.getElementById('form-login');
const formCadastro = document.getElementById('form-cadastro');
const abaLogin = document.getElementById('aba-login');
const abaCadastro = document.getElementById('aba-cadastro');
const mensagem = document.getElementById('mensagem');

/** Só aceita voltar para páginas locais (evita open redirect). */
function destino() {
  const voltar = new URLSearchParams(location.search).get('voltar');
  return voltar && /^[a-z-]+\.html(\?[\w=&]*)?$/.test(voltar) ? voltar : 'index.html';
}

function mostrarAba(cadastro) {
  formLogin.hidden = cadastro;
  formCadastro.hidden = !cadastro;
  abaLogin.setAttribute('aria-selected', String(!cadastro));
  abaCadastro.setAttribute('aria-selected', String(cadastro));
  alerta(mensagem, '');
}

abaLogin.addEventListener('click', () => mostrarAba(false));
abaCadastro.addEventListener('click', () => mostrarAba(true));
if (location.hash === '#cadastro') mostrarAba(true);

function dados(form) {
  return Object.fromEntries(new FormData(form).entries());
}

async function enviar(form, url) {
  alerta(mensagem, '');
  mostrarErrosDeCampo(form, {});
  const botao = form.querySelector('button[type=submit]');
  await comBotaoOcupado(botao, 'Aguarde…', async () => {
    try {
      await api.post(url, dados(form));
      location.href = destino();
    } catch (e) {
      mostrarErrosDeCampo(form, e.erros);
      alerta(mensagem, e.message);
    }
  });
}

formLogin.addEventListener('submit', (ev) => {
  ev.preventDefault();
  enviar(formLogin, '/api/auth/login');
});

formCadastro.addEventListener('submit', (ev) => {
  ev.preventDefault();
  enviar(formCadastro, '/api/auth/cadastro');
});
