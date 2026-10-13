import { readdir, readFile } from 'node:fs/promises';
import { resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
const root = fileURLToPath(new URL('../', import.meta.url));
const adapter = resolve(root, '../enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter');
const documented = new Set([...((await readFile(resolve(root, 'README.md'), 'utf8')).matchAll(/\| (GET|POST|PUT|DELETE) `([^`]+)`/g))].map(([, method, path]) => `${method} ${path}`));
const actual = new Set();
async function scan(directory) {
  for (const entry of await readdir(directory, { withFileTypes: true })) {
    const path = resolve(directory, entry.name);
    if (entry.isDirectory()) { await scan(path); continue; }
    if (!entry.name.endsWith('Controller.java')) continue;
    const source = await readFile(path, 'utf8');
    const base = source.match(/@RequestMapping\(\s*(?:value\s*=\s*)?"([^"]+)"/)?.[1];
    if (!base) throw new Error(`Missing controller base path: ${entry.name}`);
    for (const [, verb, options = ''] of source.matchAll(/^\s*@(Get|Post|Put|Delete)Mapping(?:\(([^\n]*)\))?/gm)) {
      const suffix = options.match(/^\s*(?:value\s*=\s*)?"([^"]+)"/)?.[1] || '';
      actual.add(`${verb.toUpperCase()} ${base}${suffix}`);
    }
  }
}
await scan(adapter);
for (const operation of actual) if (!documented.has(operation)) throw new Error(`Backend operation lacks a frontend coverage entry: ${operation}`);
for (const operation of documented) if (!actual.has(operation)) throw new Error(`Frontend coverage entry has no backend operation: ${operation}`);
console.log(`Frontend coverage inventory matches ${actual.size} backend REST operations: PASS`);
