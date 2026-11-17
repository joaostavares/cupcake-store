// TELA06 — Meus pedidos (CU06).
import { api, formatar, selo } from './api.js';
import { alerta, exigirLogin } from './layout.js';

const lista = document.getElementById('lista');

async function iniciar() {
  await exigirLogin();
  try {
    const pedidos = await api.get('/api/pedidos');
    lista.innerHTML = pedidos.length ? `
      <table>
        <thead><tr><th>Pedido</th><th>Data</th><th class="num">Itens</th><th class="num">Total</th><th>Status</th><th></th></tr></thead>
        <tbody>${pedidos.map((p) => `
          <tr>
            <td>#${p.id}</td>
            <td>${formatar.dataHora(p.dataHora)}</td>
            <td class="num">${p.itens.reduce((s, i) => s + i.quantidade, 0)}</td>
            <td class="num">${formatar.moeda(p.total)}</td>
            <td>${selo(p.status)}</td>
            <td><a class="btn btn-secundario btn-pequeno" href="pedido.html?id=${p.id}">Ver</a></td>
          </tr>`).join('')}
        </tbody>
      </table>`
      : '<p class="vazio">Você ainda não fez pedidos. <a href="index.html">Ver cupcakes</a></p>';
  } catch (e) {
    lista.innerHTML = '';
    alerta(document.getElementById('mensagem'), e.message);
  }
}

iniciar();
