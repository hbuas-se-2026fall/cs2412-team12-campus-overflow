import { readFile, writeFile } from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

/**
 * 生成后补丁（Lab 5 Step 3）。
 *
 * openapi-typescript-codegen 的 axios 模板会 `import FormData from 'form-data'`，
 * 那是 Node 专用包，浏览器工程里既装不了也打不进 bundle。
 * 我们只发 JSON，不需要 multipart，因此改成浏览器原生 FormData：
 *   pnpm api:generate 之后自动执行本脚本。
 */
const here = path.dirname(fileURLToPath(import.meta.url));
const target = path.resolve(here, '../src/api/generated/core/request.ts');

let code = await readFile(target, 'utf8');

code = code.replace("import FormData from 'form-data';\n", '');
code = code.replace('value instanceof FormData', 'value instanceof globalThis.FormData');
code = code.replace(
  'const formData = new FormData();',
  'const formData = new globalThis.FormData();',
);
code = code.replace(
  "const formHeaders = typeof formData?.getHeaders === 'function' && formData?.getHeaders() || {}",
  'const formHeaders: Record<string, string> = {} // 浏览器原生 FormData 由 axios 自动补边界',
);
code = code.replace(
  'options: ApiRequestOptions, formData?: FormData',
  'options: ApiRequestOptions, _formData?: FormData',
);

await writeFile(target, code, 'utf8');
console.log('[patch-generated] 已用原生 FormData 替换 form-data 依赖');
