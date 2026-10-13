export function integer(value, label, min = 0, max = 2147483647) {
  const text = String(value).trim();
  if (!/^\d+$/.test(text) || !Number.isSafeInteger(Number(text)) || Number(text) < min || Number(text) > max) {
    throw new Error(`${label}须为 ${min}～${max} 的整数`);
  }
  return Number(text);
}
export function id(value, label = 'ID', zero = false) {
  const text = String(value ?? '').trim();
  if (!(zero ? /^(0|[1-9]\d{0,18})$/ : /^[1-9]\d{0,18}$/).test(text) || BigInt(text) > 9223372036854775807n) {
    throw new Error(`${label}须为有效的${zero ? '非负' : '正'}整数 ID`);
  }
  return text;
}
export const version = value => BigInt(id(value, '数据版本', true));
export function money(value) {
  if (!/^\d+(\.\d{1,2})?$/.test(String(value))) throw new Error('价格最多保留两位小数');
  const [yuan, cents = ''] = String(value).split('.');
  return integer(BigInt(yuan) * 100n + BigInt(cents.padEnd(2, '0')), '价格（分）');
}
export function objectJson(value, label) {
  let result;
  try { result = JSON.parse(value); } catch { throw new Error(`${label}须为有效 JSON 对象`); }
  if (!result || Array.isArray(result) || typeof result !== 'object') throw new Error(`${label}须为 JSON 对象`);
  return result;
}
export function httpsUrl(value, optional = true) {
  if (!value && optional) return '';
  let url;
  try { url = new URL(value); } catch { throw new Error('图片和外部链接须为有效 HTTPS 地址'); }
  if (url.protocol !== 'https:' || url.username || url.password || /\s/.test(value)) throw new Error('图片和外部链接须为有效 HTTPS 地址');
  return value;
}
export const lines = value => value.split('\n').map(item => item.trim()).filter(Boolean);

const spuFields = ['sn', 'name', 'caption', 'brandId', 'category1Id', 'category2Id', 'category3Id',
  'templateId', 'freightId', 'image', 'images', 'saleService', 'introduction', 'isEnableSpec', 'specItems', 'paraItems'];
const skuFields = ['id', 'sn', 'num', 'alertNum', 'price', 'weight', 'spec', 'image', 'images', 'status'];
export const newSku = () => ({ sn: '', num: 0, alertNum: 0, price: 1, weight: 0, spec: {}, image: '', images: [], status: '1' });
export function editableProduct(result) {
  return {
    spu: Object.fromEntries(spuFields.map(key => [key, result.spu[key]])),
    skuList: result.skuList.map(sku => Object.fromEntries(skuFields.filter(key => sku[key] !== undefined).map(key => [key, sku[key]]))),
    version: result.spu.version,
  };
}
export function validateProduct(command) {
  const { spu, skuList } = command;
  if (skuList.length < 1 || skuList.length > 100) throw new Error('商品必须包含 1～100 个 SKU');
  const names = Object.keys(spu.specItems);
  if (!spu.isEnableSpec && (skuList.length !== 1 || names.length)) throw new Error('未启用规格时，须保留一个 SKU 并清空规格');
  if (spu.isEnableSpec && !names.length) throw new Error('启用规格后请填写规格选项');
  if (names.length > 20 || names.some(name => !name.trim() || name.length > 120 || !Array.isArray(spu.specItems[name])
    || !spu.specItems[name].length || spu.specItems[name].length > 100 || spu.specItems[name].some(value => typeof value !== 'string' || !value.trim() || value.length > 120))) {
    throw new Error('规格选项须为规格名称到非空字符串数组的对象，最多 20 项');
  }
  if (Object.keys(spu.paraItems).length > 100 || Object.entries(spu.paraItems).some(([key, value]) => !key.trim()
    || key.length > 120 || typeof value !== 'string' || !value.trim() || value.length > 1000)) throw new Error('参数须为名称到非空文本的对象');
  const barcodes = new Set(), combinations = new Set(), ids = new Set();
  skuList.forEach((sku, index) => {
    const label = `SKU ${index + 1}`;
    if (sku.id !== undefined) { id(sku.id, `${label} ID`); if (ids.has(sku.id)) throw new Error('SKU ID 重复'); ids.add(sku.id); }
    if (barcodes.has(sku.sn.trim())) throw new Error('SKU 条码重复');
    barcodes.add(sku.sn.trim());
    const keys = Object.keys(sku.spec).sort();
    if (keys.join('\0') !== [...names].sort().join('\0') || keys.some(key => !spu.specItems[key].includes(sku.spec[key]))) {
      throw new Error(`${label} 的规格必须与商品选项一致`);
    }
    const combination = JSON.stringify(keys.map(key => [key, sku.spec[key]]));
    if (combinations.has(combination)) throw new Error('SKU 规格组合重复');
    combinations.add(combination);
  });
  return command;
}
export function productActions(product, deleted = false) {
  if (deleted) return ['detail', 'restoration', 'purge'];
  if (product.isMarketable) return ['detail', 'withdrawal'];
  return ['detail', 'edit', product.status === '1' ? 'publication' : 'approval', 'delete'];
}
export async function validateImage(file) {
  if (!file || file.size === 0 || file.size > 10 * 1024 * 1024) throw new Error('请选择非空且不超过 10 MiB 的 JPEG 或 PNG 图片');
  const bytes = new Uint8Array(await file.slice(0, 8).arrayBuffer());
  const jpeg = bytes[0] === 0xff && bytes[1] === 0xd8 && bytes[2] === 0xff;
  const png = [137, 80, 78, 71, 13, 10, 26, 10].every((value, index) => bytes[index] === value);
  if (!jpeg && !png) throw new Error('仅支持 JPEG 或 PNG 图片');
  return file;
}
