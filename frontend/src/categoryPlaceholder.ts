/**
 * Imagens ilustrativas por categoria (sem campo no backend).
 * URLs do Unsplash (parâmetros fixos) — fallback genérico quando não houver regra.
 */
const DEFAULT =
  'https://images.unsplash.com/photo-1607082348824-0a96f2a4b9da?auto=format&fit=crop&w=720&q=80'

const RULES: { match: (n: string) => boolean; url: string }[] = [
  {
    match: (n) =>
      /livro|livraria|literatura|leitura|book|e-?book|revista|hq|manga|gibi/.test(n),
    url: 'https://images.unsplash.com/photo-1544947950-fa07a98d237f?auto=format&fit=crop&w=720&q=80',
  },
  {
    match: (n) =>
      /comida|alimento|bebida|mercado|supermercado|grocery|food|restaurante|lanche|doce|snack|caf[eé]|cozinha/.test(
        n,
      ),
    url: 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?auto=format&fit=crop&w=720&q=80',
  },
  {
    match: (n) =>
      /eletr[oô]nico|tecnologia|inform[aá]tica|computador|celular|smartphone|gadget|audio|som|tv|notebook|tablet/.test(
        n,
      ),
    url: 'https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?auto=format&fit=crop&w=720&q=80',
  },
  {
    match: (n) =>
      /moda|roupa|vestu[aá]rio|cal[çc]ado|sapato|acess[oó]rio|fashion|t[êe]nis|camisa|vestido/.test(n),
    url: 'https://images.unsplash.com/photo-1445205170230-053b83016050?auto=format&fit=crop&w=720&q=80',
  },
  {
    match: (n) =>
      /casa|lar|m[oó]veis|decora|utilidade|cozinha dom|banheiro|quarto|jardim|ferramenta/.test(n),
    url: 'https://images.unsplash.com/photo-1586023492125-27b2c045efd7?auto=format&fit=crop&w=720&q=80',
  },
  {
    match: (n) =>
      /esporte|fitness|academia|corrida|bicicleta|nata|bola|campo|gym/.test(n),
    url: 'https://images.unsplash.com/photo-1534438327276-14e5300c3a48?auto=format&fit=crop&w=720&q=80',
  },
  {
    match: (n) =>
      /beleza|cosm[eé]tico|perfume|skincare|cabelo|maquiagem|higiene/.test(n),
    url: 'https://images.unsplash.com/photo-1596462502278-27bfdc403348?auto=format&fit=crop&w=720&q=80',
  },
  {
    match: (n) =>
      /brinquedo|infantil|crian[çc]a|beb[eê]|kids|toy/.test(n),
    url: 'https://images.unsplash.com/photo-1558060370-d644479cb6f7?auto=format&fit=crop&w=720&q=80',
  },
  {
    match: (n) =>
      /auto|carro|moto|ve[ií]culo|pe[çc]a|oficina/.test(n),
    url: 'https://images.unsplash.com/photo-1492144534655-ae79c964c9d7?auto=format&fit=crop&w=720&q=80',
  },
  {
    match: (n) =>
      /sa[uú]de|farm[aá]cia|medicamento|vitamina|bem estar|wellness/.test(n),
    url: 'https://images.unsplash.com/photo-1584308666744-24d5c474f2ae?auto=format&fit=crop&w=720&q=80',
  },
  {
    match: (n) => /pet|cachorro|gato|animal|aqu[aá]rio|racao|ra[çc][aã]o/.test(n),
    url: 'https://images.unsplash.com/photo-1583511655857-d19b40a7a54e?auto=format&fit=crop&w=720&q=80',
  },
  {
    match: (n) => /papelaria|escrit[oó]rio|caneta|caderno|arte|pintura/.test(n),
    url: 'https://images.unsplash.com/photo-1513542789411-b6a5d4f31634?auto=format&fit=crop&w=720&q=80',
  },
]

function normalizeCategoryName(name: string | null | undefined): string {
  return String(name || '')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
    .trim()
}

export function placeholderImageForCategory(categoryName: string | null | undefined): string {
  const n = normalizeCategoryName(categoryName)
  if (!n) return DEFAULT
  for (const rule of RULES) {
    if (rule.match(n)) return rule.url
  }
  return DEFAULT
}
