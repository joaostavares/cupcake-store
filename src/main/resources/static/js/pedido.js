// TELA03 — Pagamento PIX + resumo do pedido (CU05, CU06, CU07, CU10).
import { api, esc, formatar, selo, STATUS_PAGAMENTO } from './api.js';
import { alerta, comBotaoOcupado, exigirLogin, toast } from './layout.js';

const conteudo = document.getElementById('conteudo');
const mensagem = document.getElementById('mensagem');
const id = Number(new URLSearchParams(location.search).get('id'));

function blocoPix(p) {
  if (p.status !== 'AGUARDANDO_PAGAMENTO' || !p.pagamento) return '';
  return `
    <section class="painel pix">
      <h2>Pague com PIX</h2>
      <img src="/api/pedidos/${p.id}/pagamento/qrcode" alt="QR Code PIX do pedido ${p.id}">
      <label for="pix-codigo">PIX copia e cola</label>
      <textarea id="pix-codigo" rows="4" readonly>${esc(p.pagamento.codigoPix)}</textarea>
      <button type="button" class="btn btn-secundario" id="btn-copiar">Copiar código</button>
      <div class="alerta alerta-aviso">Ambiente de demonstração: este PIX é <strong>simulado</strong>. Não pague.
        Use o botão abaixo para simular a confirmação do banco.</div>
      <div class="acoes">
        <button type="button" class="btn" id="btn-confirmar">Simular pagamento PIX</button>
        <button type="button" class="btn btn-perigo" id="btn-cancelar">Cancelar pedido</button>
      </div>
    </section>`;
}

function render(p) {
  document.title = `Pedido #${p.id} · Cupcakes Gourmet`;
  const pg = p.pagamento;
  conteudo.innerHTML = `
    <h1>Pedido #${p.id} ${selo(p.status)}</h1>
    <div class="layout-2">
      <section class="painel">
        <h2>Resumo do pedido</h2>
        <div class="tabela-wrap">
          <table>
            <thead><tr><th>Item</th><th class="num">Qtd</th><th class="num">Preço unit.</th><th class="num">Subtotal</th></tr></thead>
            <tbody>${p.itens.map((i) => `
              <tr><td>${esc(i.sabor)}</td><td class="num">${i.quantidade}</td>
              <td class="num">${formatar.moeda(i.precoUnitario)}</td><td class="num">${formatar.moeda(i.subtotal)}</td></tr>`).join('')}
            </tbody>
          </table>
        </div>
        <div class="resumo">
          <div class="linha"><span>Subtotal</span><span>${formatar.moeda(p.subtotal)}</span></div>
          <div class="linha"><span>Frete</span><span>${Number(p.frete) === 0 ? 'Grátis' : formatar.moeda(p.frete)}</span></div>
          <div class="linha total"><span>Total</span><span>${formatar.moeda(p.total)}</span></div>
        </div>
        <p><strong>Data:</strong> ${formatar.dataHora(p.dataHora)}<br>
           <strong>Entrega:</strong> ${esc(p.enderecoEntrega)}<br>
           <strong>Pagamento:</strong> ${pg ? `${esc(pg.metodo)} — ${selo(pg.status, STATUS_PAGAMENTO)}` : '—'}
           ${pg && pg.confirmadoEm ? `<br><strong>Pago em:</strong> ${formatar.dataHora(pg.confirmadoEm)}` : ''}</p>
        <a href="meus-pedidos.html">← Meus pedidos</a>
      </section>
      ${blocoPix(p)}
    </div>`;
  ligarBotoes(p);
}

function ligarBotoes(p) {
  const copiar = document.getElementById('btn-copiar');
  if (copiar) {
    copiar.addEventListener('click', async () => {
      const campo = document.getElementById('pix-codigo');
      try {
        await navigator.clipboard.writeText(campo.value);
      } catch {
        campo.select();
        document.execCommand('copy');
      }
      toast('Código PIX copiado');
    });
  }
  const confirmar = document.getElementById('btn-confirmar');
  if (confirmar) {
    confirmar.addEventListener('click', () => acao(confirmar, 'Confirmando…',
      `/api/pedidos/${p.id}/pagamento/confirmar`, 'Pagamento confirmado!'));
  }
  const cancelar = document.getElementById('btn-cancelar');
  if (cancelar) {
    cancelar.addEventListener('click', () => {
      if (confirm('Deseja realmente cancelar este pedido?')) {
        acao(cancelar, 'Cancelando…', `/api/pedidos/${p.id}/cancelar`, 'Pedido cancelado');
      }
    });
  }
}

async function acao(botao, texto, url, sucesso) {
  alerta(mensagem, '');
  await comBotaoOcupado(botao, texto, async () => {
    try {
      render(await api.post(url));
      alerta(mensagem, sucesso, 'sucesso');
    } catch (e) {
      alerta(mensagem, e.message);
    }
  });
}

async function iniciar() {
  await exigirLogin();
  if (!id) {
    conteudo.innerHTML = '';
    alerta(mensagem, 'Pedido não encontrado.');
    return;
  }
  try {
    render(await api.get(`/api/pedidos/${id}`));
    if (new URLSearchParams(location.search).get('novo')) {
      alerta(mensagem, 'Pedido registrado! Agora é só pagar com PIX.', 'sucesso');
    }
  } catch (e) {
    conteudo.innerHTML = '';
    alerta(mensagem, e.message);
  }
}

iniciar();
