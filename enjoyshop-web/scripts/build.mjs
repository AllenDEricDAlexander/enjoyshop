import { readFile, writeFile, mkdir, rm, copyFile } from 'node:fs/promises';
import { createHash } from 'node:crypto';
import { dirname, resolve, relative, extname, basename } from 'node:path';
import { fileURLToPath } from 'node:url';
const root = fileURLToPath(new URL('../', import.meta.url));
const output = resolve(root, 'dist');
await rm(output, { recursive: true, force: true });
await mkdir(output, { recursive: true });
const built = new Map();
async function emit(path) {
  if (built.has(path)) return built.get(path);
  let code = await readFile(path, 'utf8');
  const dependencies = [...code.matchAll(/from\s+['"](\.[^'"]+)['"]/g)];
  for (const match of dependencies) {
    const target = await emit(resolve(dirname(path), match[1]));
    let importPath = relative(dirname(relative(root, path)), target).replaceAll('\\', '/');
    if (!importPath.startsWith('.')) importPath = `./${importPath}`;
    code = code.replace(match[0], match[0].replace(match[1], importPath));
  }
  const hash = createHash('sha256').update(code).digest('hex').slice(0, 12);
  const extension = extname(path);
  const name = `${basename(path, extension)}.${hash}${extension}`;
  const target = resolve(output, dirname(relative(root, path)), name);
  await mkdir(dirname(target), { recursive: true });
  await writeFile(target, code);
  const result = relative(output, target).replaceAll('\\', '/');
  built.set(path, result);
  return result;
}
let html = await readFile(resolve(root, 'index.html'), 'utf8');
for (const asset of ['src/app.js', 'src/style.css']) {
  const path = await emit(resolve(root, asset));
  html = html.replace(`./${asset}`, `./${path}`);
}
await writeFile(resolve(output, 'index.html'), html);
await copyFile(resolve(root, 'config.js'), resolve(output, 'config.js'));
console.log(`Built ${built.size} content-hashed assets to enjoyshop-web/dist (no server started)`);
