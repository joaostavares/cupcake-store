// TELA02 — Checkout (CU01). O servidor revalida estoque, congela preços e gera o PIX.
import { api, esc, formatar } from './api.js';
import { carrinho } from './carrinho.js';
import { alerta, comBotaoOcupado, exigirLogin, mostrarErrosDeCampo } from './layout.js';

const mensagem = document.getElementById('mensagem');
const conteudo = document.getElementById('conteudo');
const form = document.getElementById('form-checkout');

async function iniciar() {
  const usuario = await exigirLogin();
  if (!carrinho.itens().length) {
    alerta(mensagem, 'Seu carrinho está vazio.', 'info');
    return;
  }
  try {
    const cot = await api.post('/api/pedidos/cotacao', { itens: carrinho.paraApi() });
    document.getElementById('itens').innerHTML = `
      <table>
        <thead><tr><th>Item</th><th class="num">Qtd</th><th class="num">Subtotal</th></tr></thead>
        <tbody>${cot.itens.map((i) => `
          <tr><td>${esc(i.sabor)}</td><td class="num">${i.quantidade}</td><td class="num">${formatar.moeda(i.subtotal)}</td></tr>`).join('')}
        </tbody>
      </table>`;
    document.getElementById('subtotal').textContent = formatar.moeda(cot.subtotal);
    document.getElementById('frete').textContent = Number(cot.frete) === 0 ? 'Grátis' : formatar.moeda(cot.frete);
    document.getElementById('total').textContent = formatar.moeda(cot.total);
    form.elements.enderecoEntrega.value = usuario.endereco || '';
    conteudo.hidden = false;
  } catch (e) {
    alerta(mensagem, e.message);
    mensagem.insertAdjacentHTML('beforeend', ' <a href="carrinho.html">Voltar ao carrinho</a>');
  }
}

form.addEventListener('submit', async (ev) => {
  ev.preventDefault();
  alerta(mensagem, '');
  mostrarErrosDeCampo(form, {});
  await comBotaoOcupado(document.getElementById('btn-pagar'), 'Processando…', async () => {
    try {
      const pedido = await api.post('/api/pedidos', {
        itens: carrinho.paraApi(),
        enderecoEntrega: form.elements.enderecoEntrega.value,
      });
      carrinho.limpar();
      location.href = `pedido.html?id=${pedido.id}&novo=1`;
    } catch (e) {
      if (e.status === 401) {
        location.href = 'login.html?voltar=checkout.html';
        return;
      }
      mostrarErrosDeCampo(form, e.erros);
      alerta(mensagem, e.message);
      if (e.status === 409) {
        mensagem.insertAdjacentHTML('beforeend', ' <a href="carrinho.html">Ajustar carrinho</a>');
      }
    }
  });
});

iniciar();
