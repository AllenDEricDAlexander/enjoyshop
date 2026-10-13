import { readdir, readFile, access } from 'node:fs/promises';
import { resolve, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';
import { execFileSync } from 'node:child_process';
const root = fileURLToPath(new URL('../', import.meta.url));
async function check(directory) {
  for (const entry of await readdir(directory, { withFileTypes: true })) {
    const path = resolve(directory, entry.name);
    if (entry.isDirectory()) await check(path);
    else if (/\.(?:js|mjs)$/.test(entry.name)) {
      execFileSync(process.execPath, ['--check', path], { stdio: 'inherit' });
      const code = await readFile(path, 'utf8');
      for (const match of code.matchAll(/(?:from\s+|import\s*)['"](\.[^'"]+)['"]/g)) await access(resolve(dirname(path), match[1]));
    }
  }
}
await check(resolve(root, 'src'));
await check(resolve(root, 'scripts'));
await check(resolve(root, 'test'));
execFileSync(process.execPath, ['--check', resolve(root, 'config.js')], { stdio: 'inherit' });
const html = await readFile(resolve(root, 'index.html'), 'utf8');
for (const match of html.matchAll(/(?:src|href)="(\.\/[^"#]+)"/g)) await access(resolve(root, match[1]));
console.log('JavaScript syntax, module imports and HTML assets: PASS');
