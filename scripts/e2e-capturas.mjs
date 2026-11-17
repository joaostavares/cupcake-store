// Roteiro E2E no navegador (Playwright/Chromium) que percorre os casos de uso e salva capturas de tela
// em docs/situacao-3/evidencias/. Também falha se houver erro de JavaScript no console.
//
// Uso (com a aplicação rodando em http://localhost:8080 e banco recém-criado):
//   npm i playwright@1.65.1 && npx playwright install chromium-headless-shell
//   node scripts/e2e-capturas.mjs [baseUrl]
import { chromium } from 'playwright';
import { mkdirSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const BASE = process.argv[2] || 'http://localhost:8080';
const OUT = process.env.EVIDENCIAS_DIR || join(dirname(fileURLToPath(import.meta.url)), '..', 'docs', 'situacao-3', 'evidencias');
mkdirSync(OUT, { recursive: true });

const errosConsole = [];
const resultados = [];

function verificar(id, descricao, condicao) {
  resultados.push({ id, descricao, ok: !!condicao });
  console.log(`${condicao ? 'OK  ' : 'FALHA'} ${id} ${descricao}`);
}

async function print(page, nome) {
  await page.waitForTimeout(250);
  await page.screenshot({ path: join(OUT, `${nome}.png`), fullPage: true });
}

function monitorar(page) {
  page.on('pageerror', (e) => errosConsole.push(`${page.url()}: ${e.message}`));
  page.on('console', (m) => {
    // 401/403/404/409 esperados nos cenários de erro aparecem como "Failed to load resource" — não são bugs de JS.
    if (m.type() === 'error' && !m.text().includes('Failed to load resource')) errosConsole.push(`${page.url()}: ${m.text()}`);
  });
}

const browser = await chromium.launch();
const ctx = await browser.newContext({ viewport: { width: 1280, height: 800 }, locale: 'pt-BR' });
const page = await ctx.newPage();
monitorar(page);
page.on('dialog', (d) => d.accept());

// CT01 — Vitrine pública
await page.goto(`${BASE}/index.html`);
await page.waitForSelector('.card');
verificar('CT01', 'Vitrine pública lista cupcakes sem login', (await page.locator('.card').count()) >= 8);
verificar('CT02', 'Cupcake sem estoque aparece como Esgotado e sem compra',
  await page.locator('.card.esgotado button[disabled]').count() === 1);
await print(page, 'T01-vitrine');

// CT03 — Adicionar ao carrinho
const card = (sabor) => page.locator('.card', { hasText: sabor });
await card('Red Velvet').locator('[data-acao=mais]').click();
await card('Red Velvet').locator('[data-acao=mais]').click();
await card('Red Velvet').locator('[data-acao=adicionar]').click();
await card('Pistache').locator('[data-acao=mais]').click();
await card('Pistache').locator('[data-acao=adicionar]').click();
verificar('CT03', 'Adicionar ao carrinho atualiza contador (5 itens)', (await page.textContent('#contador-carrinho')) === '5');

// CT04 — Acima do estoque (Pistache tem 10; já há 2 no carrinho)
await card('Pistache').locator('input').fill('9');
await card('Pistache').locator('[data-acao=adicionar]').click();
await page.waitForSelector('#toast.erro');
verificar('CT04', 'Adicionar acima do estoque exibe MSG04',
  (await page.textContent('#toast')).includes('Só temos 10 unidade(s) de Pistache'));
await print(page, 'T02-msg04-estoque-carrinho');

// CT05 — Carrinho com cotação do servidor
await page.goto(`${BASE}/carrinho.html`);
await page.waitForSelector('#painel-resumo:not([hidden])');
await page.waitForFunction(() => document.getElementById('total').textContent.includes('R$'));
verificar('CT05', 'Carrinho: subtotal 70,50 + frete 10,00 = total 80,50 (RN02)',
  (await page.textContent('#total')).replace(/\s/g, ' ').includes('80,50'));
await print(page, 'T03-carrinho');

// CT06 — Finalizar sem login redireciona ao login; senha errada mostra MSG01
await page.click('#btn-finalizar');
await page.waitForURL(/login\.html\?voltar=checkout\.html/);
await page.fill('#login-email', 'cliente@cupcakes.com');
await page.fill('#login-senha', 'errada');
await page.click('#form-login button[type=submit]');
await page.waitForSelector('#mensagem:not([hidden])');
verificar('CT06', 'Login inválido exibe MSG01', (await page.textContent('#mensagem')).includes('E-mail ou senha inválidos.'));
await print(page, 'T04-msg01-login-invalido');

// CT07 — Login correto volta para o checkout com endereço do cadastro
await page.fill('#login-senha', 'Cliente@123');
await page.click('#form-login button[type=submit]');
await page.waitForURL(/checkout\.html/);
await page.waitForSelector('#conteudo:not([hidden])');
verificar('CT07', 'Após login volta ao checkout com endereço pré-preenchido',
  (await page.inputValue('#enderecoEntrega')).includes('Rua das Flores'));
await print(page, 'T05-checkout');

// CT08 — Pagar com PIX cria o pedido e mostra QR Code
await page.click('#btn-pagar');
await page.waitForURL(/pedido\.html\?id=\d+/);
await page.waitForSelector('#pix-codigo');
// Aguarda o PNG do QR Code carregar (a 1ª versão do roteiro checava antes do carregamento — ver laudo, D01).
const qrOk = await page.waitForFunction(() => {
  const img = document.querySelector('.pix img');
  return img && img.complete && img.naturalWidth > 0;
}, null, { timeout: 5000 }).then(() => true, () => false);
verificar('CT08', 'Pedido criado (AGUARDANDO_PAGAMENTO) com QR Code e PIX copia e cola',
  qrOk && (await page.inputValue('#pix-codigo')).startsWith('000201'));
verificar('CT09', 'Carrinho esvaziado após o pedido', (await page.textContent('#contador-carrinho')) === '0');
await print(page, 'T06-pix-resumo');
const pedidoId = new URL(page.url()).searchParams.get('id');

// CT10 — Simular pagamento
await page.click('#btn-confirmar');
await page.waitForSelector('.status-PAGO');
verificar('CT10', 'Simular pagamento muda o pedido para Pago', (await page.textContent('#mensagem')).includes('Pagamento confirmado'));
await print(page, 'T07-pagamento-confirmado');

// CT11 — Meus pedidos
await page.goto(`${BASE}/meus-pedidos.html`);
await page.waitForSelector('table');
verificar('CT11', 'Meus pedidos lista o pedido pago', (await page.textContent('#lista')).includes(`#${pedidoId}`));
await print(page, 'T08-meus-pedidos');

// CT12 — Estoque acaba entre o carrinho e o checkout (MSG05)
await page.goto(`${BASE}/index.html`);
await page.waitForSelector('.card');
await card('Limão Siciliano').locator('[data-acao=adicionar]').click();
const admin = await browser.newContext({ viewport: { width: 1280, height: 800 }, locale: 'pt-BR' });
const pa = await admin.newPage();
monitorar(pa);
pa.on('dialog', (d) => d.accept());
await pa.goto(`${BASE}/login.html`);
await pa.fill('#login-email', 'admin@cupcakes.com');
await pa.fill('#login-senha', 'Admin@123');
await pa.click('#form-login button[type=submit]');
await pa.waitForURL(/index\.html/);
await pa.goto(`${BASE}/admin.html`);
await pa.waitForSelector('#tabela-cupcakes table');
const linhaLimao = pa.locator('#tabela-cupcakes tr', { hasText: 'Limão Siciliano' });
await linhaLimao.locator('.estoque-input').fill('0');
await linhaLimao.locator('[data-acao=estoque]').click();
await pa.waitForSelector('#toast:not([hidden])');
await page.goto(`${BASE}/checkout.html`);
await page.waitForSelector('#conteudo:not([hidden])');
await page.click('#btn-pagar');
await page.waitForSelector('#mensagem:not([hidden])');
verificar('CT12', 'Estoque zerado antes do checkout exibe MSG05 e não cria pedido',
  (await page.textContent('#mensagem')).includes('Estoque insuficiente para Limão Siciliano: disponível 0, solicitado 1.'));
await print(page, 'T09-msg05-estoque-checkout');
await linhaLimao.locator('.estoque-input').fill('15');
await linhaLimao.locator('[data-acao=estoque]').click();

// CT13 — Admin: catálogo e validação de formulário
await pa.fill('#f-sabor', '');
await pa.fill('#f-preco', '0');
await pa.fill('#f-estoque', '-1');
await pa.click('#form-cupcake button[type=submit]');
await pa.waitForSelector('#form-cupcake .erro-campo');
verificar('CT13', 'Admin: formulário inválido destaca campos (MSG03)', (await pa.locator('#form-cupcake .erro-campo').count()) >= 3);
await print(pa, 'T10-admin-catalogo-validacao');

await pa.fill('#f-sabor', 'Morango com Nutella');
await pa.fill('#f-descricao', 'Massa de baunilha com recheio de Nutella e morangos frescos.');
await pa.fill('#f-preco', '14.90');
await pa.fill('#f-estoque', '12');
await pa.click('#form-cupcake button[type=submit]');
await pa.waitForSelector('#tabela-cupcakes tr:has-text("Morango com Nutella")');
verificar('CT14', 'Admin cadastra novo cupcake', true);

// CT15 — Admin movimenta pedido
await pa.click('#aba-pedidos');
await pa.waitForSelector('#tabela-pedidos table');
const linhaPedido = pa.locator(`#tabela-pedidos tr[data-id="${pedidoId}"]`);
await linhaPedido.locator('[data-status=EM_PREPARO]').click();
await pa.waitForSelector(`#tabela-pedidos tr[data-id="${pedidoId}"] .status-EM_PREPARO`);
verificar('CT15', 'Admin avança pedido de Pago para Em preparo', true);
await print(pa, 'T11-admin-pedidos');

// CT16 — Cliente não acessa admin
await page.goto(`${BASE}/admin.html`);
await page.waitForSelector('.alerta-erro');
verificar('CT16', 'Cliente na página de admin vê MSG07', (await page.textContent('main')).includes('Acesso restrito ao administrador.'));

// CT17 — Cadastro com dados inválidos
const novo = await browser.newContext({ viewport: { width: 1280, height: 800 }, locale: 'pt-BR' });
const pn = await novo.newPage();
monitorar(pn);
await pn.goto(`${BASE}/login.html#cadastro`);
await pn.fill('#cad-email', 'cliente@cupcakes.com');
await pn.fill('#cad-nome', 'Fulano');
await pn.fill('#cad-senha', '123');
await pn.click('#form-cadastro button[type=submit]');
await pn.waitForSelector('#form-cadastro .erro-campo');
verificar('CT17', 'Cadastro com senha curta destaca o campo (MSG03)',
  (await pn.textContent('#form-cadastro')).includes('A senha deve ter entre 6 e 72 caracteres.'));
await pn.fill('#cad-senha', '123456');
await pn.click('#form-cadastro button[type=submit]');
await pn.waitForFunction(() => document.getElementById('mensagem').textContent.includes('já está cadastrado'));
verificar('CT18', 'Cadastro com e-mail existente exibe MSG02', true);
await print(pn, 'T12-msg02-cadastro-duplicado');

// CT21 — Admin altera o preço com o item já no carrinho do cliente (defeito D04)
await page.goto(`${BASE}/index.html`);
await page.waitForSelector('.card');
await card('Red Velvet').locator('[data-acao=adicionar]').click();
await pa.click('#aba-catalogo');
await pa.locator('#tabela-cupcakes tr', { hasText: 'Red Velvet' }).locator('[data-acao=editar]').click();
await pa.fill('#f-preco', '13.90');
await pa.click('#form-cupcake button[type=submit]');
await pa.waitForSelector('#tabela-cupcakes tr:has-text("13,90")');
await page.goto(`${BASE}/carrinho.html`);
await page.waitForFunction(() => document.getElementById('total').textContent.includes('R$'));
const linhaRed = (await page.locator('#itens tr', { hasText: 'Red Velvet' }).textContent()).replace(/\s+/g, ' ');
verificar('CT21', 'Carrinho mostra o preço atual após o admin alterar o preço (D04)', linhaRed.includes('13,90'));
await print(page, 'T14-carrinho-preco-atualizado');

// CT19 — Responsivo (celular)
const mobile = await browser.newContext({ viewport: { width: 390, height: 844 }, isMobile: true, locale: 'pt-BR' });
const pm = await mobile.newPage();
monitorar(pm);
await pm.goto(`${BASE}/index.html`);
await pm.waitForSelector('.card');
const semRolagemLateral = await pm.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth);
verificar('CT19', 'Vitrine no celular (390px) sem rolagem horizontal', semRolagemLateral);
await pm.screenshot({ path: join(OUT, 'T13-vitrine-celular.png') });

verificar('CT20', 'Nenhum erro de JavaScript no console', errosConsole.length === 0);
if (errosConsole.length) console.log(errosConsole.join('\n'));

await browser.close();
const falhas = resultados.filter((r) => !r.ok);
console.log(`\n${resultados.length - falhas.length}/${resultados.length} verificações OK`);
process.exit(falhas.length ? 1 : 0);
