import js from '@eslint/js';
import prettier from 'eslint-config-prettier';
import reactHooks from 'eslint-plugin-react-hooks';
import globals from 'globals';
import tseslint from 'typescript-eslint';

/**
 * ESLint 9 扁平配置。
 *
 * 关键约定：
 *  - src/api/generated 是 openapi-typescript-codegen 的产物，不参与 lint（产物自带 any）
 *  - 业务代码禁止显式 any（Lab 5：TypeScript 严格模式无 any）
 *  - prettier 放最后，关闭与格式化冲突的规则
 */
export default tseslint.config(
  {
    ignores: ['dist', 'node_modules', 'src/api/generated', 'playwright-report', 'test-results'],
  },
  {
    files: ['**/*.{ts,tsx}'],
    extends: [js.configs.recommended, ...tseslint.configs.recommended],
    languageOptions: {
      ecmaVersion: 2022,
      globals: { ...globals.browser },
    },
    plugins: {
      'react-hooks': reactHooks,
    },
    rules: {
      ...reactHooks.configs.recommended.rules,
      '@typescript-eslint/no-explicit-any': 'error',
      '@typescript-eslint/no-unused-vars': ['error', { argsIgnorePattern: '^_' }],
      'no-console': ['warn', { allow: ['warn', 'error'] }],
    },
  },
  {
    files: ['*.config.{js,ts}', 'eslint.config.js', 'e2e/**/*.ts'],
    languageOptions: {
      globals: { ...globals.node },
    },
  },
  prettier,
);
