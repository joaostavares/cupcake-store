// TELA01 — Vitrine pública (CU02) e adicionar ao carrinho (CU03, RN01).
import { api, esc, formatar, imagem } from './api.js';
import { carrinho, MAX_POR_ITEM } from './carrinho.js';
import { alerta, toast } from './layout.js';

const vitrine = document.getElementById('vitrine');
const mensagem = document.getElementById('mensagem');
let cupcakes = [];

function card(c) {
  const esgotado = c.estoque <= 0;
  const max = Math.min(c.estoque, MAX_POR_ITEM);
  const selo = esgotado
    ? '<span class="selo selo-esgotado">Esgotado</span>'
    : c.estoque <= 5
      ? `<span class="selo selo-poucas">Últimas ${c.estoque} unidades</span>`
      : `<span class="selo">${c.estoque} disponíveis</span>`;
  return `
    <article class="card ${esgotado ? 'esgotado' : ''}" data-id="${c.id}">
      <img src="${imagem(c.imagemUrl)}" alt="Cupcake sabor ${esc(c.sabor)}" loading="lazy">
      <div class="card-corpo">
        <h3>${esc(c.sabor)}</h3>
        <p class="descricao">${esc(c.descricao || '')}</p>
        <div class="linha"><span class="preco">${formatar.moeda(c.preco)}</span>${selo}</div>
        ${esgotado ? '<button class="btn btn-bloco" type="button" disabled>Indisponível</button>' : `
        <div class="linha">
          <div class="qtd">
            <button type="button" data-acao="menos" aria-label="Diminuir quantidade de ${esc(c.sabor)}">−</button>
            <input type="number" value="1" min="1" max="${max}" aria-label="Quantidade de ${esc(c.sabor)}">
            <button type="button" data-acao="mais" aria-label="Aumentar quantidade de ${esc(c.sabor)}">+</button>
          </div>
          <button class="btn" type="button" data-acao="adicionar">Adicionar ao carrinho</button>
        </div>`}
      </div>
    </article>`;
}

async function carregar() {
  try {
    cupcakes = await api.get('/api/cupcakes');
    vitrine.innerHTML = cupcakes.length
      ? cupcakes.map(card).join('')
      : '<p class="vazio">Nenhum cupcake disponível no momento.</p>';
  } catch (e) {
    vitrine.innerHTML = '';
    alerta(mensagem, e.message);
  }
}

vitrine.addEventListener('click', (ev) => {
  const botao = ev.target.closest('button[data-acao]');
  if (!botao) return;
  const cardEl = botao.closest('.card');
  const cupcake = cupcakes.find((c) => c.id === Number(cardEl.dataset.id));
  const input = cardEl.querySelector('input');
  const max = Number(input.max);
  let qtd = Number(input.value) || 1;

  if (botao.dataset.acao === 'menos') input.value = Math.max(1, qtd - 1);
  if (botao.dataset.acao === 'mais') input.value = Math.min(max, qtd + 1);
  if (botao.dataset.acao === 'adicionar') {
    qtd = Math.max(1, Math.floor(qtd));
    const erro = carrinho.adicionar(cupcake, qtd);
    if (erro) {
      toast(erro, 'erro');
    } else {
      toast(`${qtd}× ${cupcake.sabor} adicionado ao carrinho`);
      input.value = 1;
    }
  }
});

carregar();
