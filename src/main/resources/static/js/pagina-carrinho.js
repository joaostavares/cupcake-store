// TELA05 — Carrinho (CU03). Valores calculados pelo servidor via /api/pedidos/cotacao (RN02).
import { api, esc, formatar, imagem } from './api.js';
import { carrinho, MAX_POR_ITEM } from './carrinho.js';
import { alerta, usuarioLogado } from './layout.js';

const itensEl = document.getElementById('itens');
const painelResumo = document.getElementById('painel-resumo');
const mensagem = document.getElementById('mensagem');
const btnFinalizar = document.getElementById('btn-finalizar');

function linha(i, estoque) {
  const semEstoque = estoque !== undefined && i.quantidade > estoque;
  return `
    <tr data-id="${i.cupcakeId}">
      <td><img class="miniatura" src="${imagem(i.imagemUrl)}" alt=""></td>
      <td>${esc(i.sabor)}${semEstoque ? `<br><span class="selo selo-esgotado">Disponível: ${estoque}</span>` : ''}</td>
      <td class="num">${formatar.moeda(i.preco)}</td>
      <td>
        <div class="qtd">
          <button type="button" data-acao="menos" aria-label="Diminuir" ${i.quantidade <= 1 ? 'disabled' : ''}>−</button>
          <input type="number" value="${i.quantidade}" min="1" max="${MAX_POR_ITEM}" aria-label="Quantidade de ${esc(i.sabor)}">
          <button type="button" data-acao="mais" aria-label="Aumentar" ${i.quantidade >= Math.min(MAX_POR_ITEM, estoque ?? MAX_POR_ITEM) ? 'disabled' : ''}>+</button>
        </div>
      </td>
      <td class="num">${formatar.moeda(i.preco * i.quantidade)}</td>
      <td><button type="button" class="btn btn-perigo btn-pequeno" data-acao="remover">Remover</button></td>
    </tr>`;
}

async function renderizar() {
  let itens = carrinho.itens();
  alerta(mensagem, '');
  if (!itens.length) {
    itensEl.innerHTML = '<p class="vazio">Seu carrinho está vazio. <a href="index.html">Ver cupcakes</a></p>';
    painelResumo.hidden = true;
    return;
  }

  let cotacao = null;
  try {
    cotacao = await api.post('/api/pedidos/cotacao', { itens: carrinho.paraApi() });
  } catch (e) {
    alerta(mensagem, e.message + (e.status === 404 || e.status === 409 ? ' Remova o item do carrinho para continuar.' : ''));
  }

  if (cotacao && carrinho.sincronizar(cotacao.itens)) {
    itens = carrinho.itens();
  }

  const estoques = {};
  let faltaEstoque = false;
  if (cotacao) {
    cotacao.itens.forEach((c) => {
      estoques[c.cupcakeId] = c.estoqueDisponivel;
      const local = itens.find((i) => i.cupcakeId === c.cupcakeId);
      if (local && local.quantidade > c.estoqueDisponivel) faltaEstoque = true;
    });
  }

  itensEl.innerHTML = `
    <table>
      <thead><tr><th></th><th>Sabor</th><th class="num">Preço</th><th>Qtd</th><th class="num">Subtotal</th><th></th></tr></thead>
      <tbody>${itens.map((i) => linha(i, estoques[i.cupcakeId])).join('')}</tbody>
    </table>`;

  painelResumo.hidden = false;
  document.getElementById('subtotal').textContent = cotacao ? formatar.moeda(cotacao.subtotal) : '—';
  document.getElementById('frete').textContent = cotacao
    ? (Number(cotacao.frete) === 0 ? 'Grátis' : formatar.moeda(cotacao.frete)) : '—';
  document.getElementById('total').textContent = cotacao ? formatar.moeda(cotacao.total) : '—';
  btnFinalizar.disabled = !cotacao || faltaEstoque;
  if (faltaEstoque) {
    alerta(mensagem, 'Alguns itens não têm estoque suficiente. Ajuste as quantidades destacadas.', 'aviso');
  }
}

itensEl.addEventListener('click', (ev) => {
  const botao = ev.target.closest('button[data-acao]');
  if (!botao) return;
  const id = Number(botao.closest('tr').dataset.id);
  const item = carrinho.itens().find((i) => i.cupcakeId === id);
  if (botao.dataset.acao === 'remover') carrinho.remover(id);
  if (botao.dataset.acao === 'menos') carrinho.alterarQuantidade(id, item.quantidade - 1);
  if (botao.dataset.acao === 'mais') carrinho.alterarQuantidade(id, item.quantidade + 1);
  renderizar();
});

itensEl.addEventListener('change', (ev) => {
  if (ev.target.matches('input[type=number]')) {
    const id = Number(ev.target.closest('tr').dataset.id);
    carrinho.alterarQuantidade(id, Math.floor(Number(ev.target.value)) || 1);
    renderizar();
  }
});

btnFinalizar.addEventListener('click', async () => {
  const usuario = await usuarioLogado();
  location.href = usuario ? 'checkout.html' : 'login.html?voltar=checkout.html';
});

renderizar();
