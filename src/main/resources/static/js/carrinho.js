// Carrinho guardado no navegador (localStorage). O servidor revalida tudo no checkout (RN05).

const CHAVE = 'cupcakes.carrinho';
export const MAX_POR_ITEM = 20;

function ler() {
  try {
    const itens = JSON.parse(localStorage.getItem(CHAVE) || '[]');
    return Array.isArray(itens) ? itens : [];
  } catch {
    return [];
  }
}

function gravar(itens) {
  try {
    localStorage.setItem(CHAVE, JSON.stringify(itens));
  } catch {
    /* modo privado / armazenamento cheio: o carrinho só vale para esta página */
  }
  document.dispatchEvent(new CustomEvent('carrinho:alterado'));
}

export const carrinho = {
  itens: ler,

  quantidadeTotal() {
    return ler().reduce((s, i) => s + i.quantidade, 0);
  },

  /** RN01/RN03: devolve mensagem de erro (MSG04) ou null se adicionou. */
  adicionar(cupcake, quantidade) {
    const itens = ler();
    const existente = itens.find((i) => i.cupcakeId === cupcake.id);
    const novaQtd = (existente ? existente.quantidade : 0) + quantidade;
    if (novaQtd > cupcake.estoque) {
      return `Só temos ${cupcake.estoque} unidade(s) de ${cupcake.sabor} disponíveis.`;
    }
    if (novaQtd > MAX_POR_ITEM) {
      return `A quantidade máxima por item é ${MAX_POR_ITEM}.`;
    }
    if (existente) {
      existente.quantidade = novaQtd;
      existente.estoque = cupcake.estoque;
      existente.preco = cupcake.preco;
    } else {
      itens.push({
        cupcakeId: cupcake.id, sabor: cupcake.sabor, preco: cupcake.preco,
        imagemUrl: cupcake.imagemUrl, estoque: cupcake.estoque, quantidade,
      });
    }
    gravar(itens);
    return null;
  },

  alterarQuantidade(cupcakeId, quantidade) {
    const itens = ler();
    const item = itens.find((i) => i.cupcakeId === cupcakeId);
    if (!item) return;
    item.quantidade = Math.max(1, Math.min(quantidade, MAX_POR_ITEM));
    gravar(itens);
  },

  remover(cupcakeId) {
    gravar(ler().filter((i) => i.cupcakeId !== cupcakeId));
  },

  limpar() {
    gravar([]);
  },

  /** Formato esperado pela API. */
  paraApi() {
    return ler().map((i) => ({ cupcakeId: i.cupcakeId, quantidade: i.quantidade }));
  },
};
