// TELA07 — Administração: catálogo/estoque (CU08) e pedidos (CU09).
import { api, esc, formatar, imagem, selo, STATUS_PEDIDO } from './api.js';
import { alerta, comBotaoOcupado, exigirLogin, mostrarErrosDeCampo, toast } from './layout.js';

const mensagem = document.getElementById('mensagem');
const form = document.getElementById('form-cupcake');
const tabelaCupcakes = document.getElementById('tabela-cupcakes');
const tabelaPedidos = document.getElementById('tabela-pedidos');
const filtro = document.getElementById('filtro-status');
let cupcakes = [];

// Próximos status permitidos (espelha RN07; o servidor valida de novo).
const PROXIMOS = {
  AGUARDANDO_PAGAMENTO: ['PAGO', 'CANCELADO'],
  PAGO: ['EM_PREPARO', 'CANCELADO'],
  EM_PREPARO: ['ENVIADO'],
  ENVIADO: ['ENTREGUE'],
};

/* ---------- Abas ---------- */
const abas = { catalogo: document.getElementById('aba-catalogo'), pedidos: document.getElementById('aba-pedidos') };
function mostrarAba(nome) {
  Object.entries(abas).forEach(([n, botao]) => {
    botao.setAttribute('aria-selected', String(n === nome));
    document.getElementById(`painel-${n}`).hidden = n !== nome;
  });
  alerta(mensagem, '');
  if (nome === 'pedidos') carregarPedidos();
}
abas.catalogo.addEventListener('click', () => mostrarAba('catalogo'));
abas.pedidos.addEventListener('click', () => mostrarAba('pedidos'));

/* ---------- Catálogo ---------- */
async function carregarCupcakes() {
  try {
    cupcakes = await api.get('/api/admin/cupcakes');
    tabelaCupcakes.innerHTML = `
      <table>
        <thead><tr><th></th><th>Sabor</th><th class="num">Preço</th><th>Estoque</th><th>Situação</th><th></th></tr></thead>
        <tbody>${cupcakes.map((c) => `
          <tr data-id="${c.id}" class="${c.ativo ? '' : 'inativo'}">
            <td><img class="miniatura" src="${imagem(c.imagemUrl)}" alt=""></td>
            <td>${esc(c.sabor)}</td>
            <td class="num">${formatar.moeda(c.preco)}</td>
            <td><input class="estoque-input" type="number" min="0" value="${c.estoque}" aria-label="Estoque de ${esc(c.sabor)}">
                <button type="button" class="btn btn-secundario btn-pequeno" data-acao="estoque">Salvar</button></td>
            <td>${c.ativo ? '<span class="selo">Ativo</span>' : '<span class="selo selo-esgotado">Inativo</span>'}</td>
            <td><button type="button" class="btn btn-secundario btn-pequeno" data-acao="editar">Editar</button></td>
          </tr>`).join('')}
        </tbody>
      </table>`;
  } catch (e) {
    alerta(mensagem, e.message);
  }
}

function limparForm() {
  form.reset();
  form.elements.id.value = '';
  form.elements.ativo.checked = true;
  form.elements.estoque.disabled = false;
  document.getElementById('dica-estoque').hidden = true;
  document.getElementById('titulo-form').textContent = 'Novo cupcake';
  mostrarErrosDeCampo(form, {});
}

tabelaCupcakes.addEventListener('click', async (ev) => {
  const botao = ev.target.closest('button[data-acao]');
  if (!botao) return;
  const linha = botao.closest('tr');
  const c = cupcakes.find((x) => x.id === Number(linha.dataset.id));
  if (botao.dataset.acao === 'editar') {
    form.elements.id.value = c.id;
    form.elements.sabor.value = c.sabor;
    form.elements.descricao.value = c.descricao || '';
    form.elements.preco.value = c.preco;
    form.elements.estoque.value = c.estoque;
    form.elements.estoque.disabled = true; // estoque se altera pela tabela (D05)
    document.getElementById('dica-estoque').hidden = false;
    form.elements.imagemUrl.value = c.imagemUrl || '';
    form.elements.ativo.checked = c.ativo;
    document.getElementById('titulo-form').textContent = `Editar: ${c.sabor}`;
    form.scrollIntoView({ behavior: 'smooth' });
  }
  if (botao.dataset.acao === 'estoque') {
    const valor = linha.querySelector('.estoque-input').value;
    await comBotaoOcupado(botao, '…', async () => {
      try {
        await api.patch(`/api/admin/cupcakes/${c.id}/estoque`, { estoque: valor === '' ? null : Number(valor) });
        toast(`Estoque de ${c.sabor} atualizado`);
        carregarCupcakes();
      } catch (e) {
        alerta(mensagem, e.message);
      }
    });
  }
});

form.addEventListener('submit', async (ev) => {
  ev.preventDefault();
  alerta(mensagem, '');
  mostrarErrosDeCampo(form, {});
  const id = form.elements.id.value;
  const corpo = {
    sabor: form.elements.sabor.value,
    descricao: form.elements.descricao.value,
    preco: form.elements.preco.value === '' ? null : Number(form.elements.preco.value),
    estoque: id || form.elements.estoque.value === '' ? null : Number(form.elements.estoque.value),
    imagemUrl: form.elements.imagemUrl.value,
    ativo: form.elements.ativo.checked,
  };
  await comBotaoOcupado(form.querySelector('button[type=submit]'), 'Salvando…', async () => {
    try {
      if (id) {
        await api.put(`/api/admin/cupcakes/${id}`, corpo);
      } else {
        await api.post('/api/admin/cupcakes', corpo);
      }
      toast('Cupcake salvo');
      limparForm();
      carregarCupcakes();
    } catch (e) {
      mostrarErrosDeCampo(form, e.erros);
      alerta(mensagem, e.message);
    }
  });
});
document.getElementById('btn-novo').addEventListener('click', limparForm);

/* ---------- Pedidos ---------- */
async function carregarPedidos() {
  tabelaPedidos.innerHTML = '<p class="carregando">Carregando…</p>';
  try {
    const url = '/api/admin/pedidos' + (filtro.value ? `?status=${filtro.value}` : '');
    const pedidos = await api.get(url);
    tabelaPedidos.innerHTML = pedidos.length ? `
      <table>
        <thead><tr><th>Pedido</th><th>Data</th><th>Cliente</th><th class="num">Total</th><th>Status</th><th>Ações</th></tr></thead>
        <tbody>${pedidos.map((p) => `
          <tr data-id="${p.id}">
            <td><a href="pedido.html?id=${p.id}">#${p.id}</a></td>
            <td>${formatar.dataHora(p.dataHora)}</td>
            <td>${esc(p.clienteNome)}<br><span class="dica">${esc(p.clienteEmail)}</span></td>
            <td class="num">${formatar.moeda(p.total)}</td>
            <td>${selo(p.status)}</td>
            <td>${(PROXIMOS[p.status] || []).map((s) => `
              <button type="button" class="btn ${s === 'CANCELADO' ? 'btn-perigo' : 'btn-secundario'} btn-pequeno" data-status="${s}">
                ${s === 'CANCELADO' ? 'Cancelar' : '→ ' + esc(STATUS_PEDIDO[s])}</button>`).join(' ') || '—'}
            </td>
          </tr>`).join('')}
        </tbody>
      </table>`
      : '<p class="vazio">Nenhum pedido encontrado.</p>';
  } catch (e) {
    tabelaPedidos.innerHTML = '';
    alerta(mensagem, e.message);
  }
}

filtro.addEventListener('change', carregarPedidos);

tabelaPedidos.addEventListener('click', async (ev) => {
  const botao = ev.target.closest('button[data-status]');
  if (!botao) return;
  const id = botao.closest('tr').dataset.id;
  const status = botao.dataset.status;
  if (status === 'CANCELADO' && !confirm(`Cancelar o pedido #${id}? O estoque será devolvido.`)) return;
  await comBotaoOcupado(botao, '…', async () => {
    try {
      await api.patch(`/api/admin/pedidos/${id}/status`, { status });
      toast(`Pedido #${id}: ${STATUS_PEDIDO[status]}`);
      carregarPedidos();
      carregarCupcakes();
    } catch (e) {
      alerta(mensagem, e.message);
    }
  });
});

exigirLogin({ admin: true }).then(carregarCupcakes);
