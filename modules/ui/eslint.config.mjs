import globals from 'globals'
import tseslint from 'typescript-eslint'
import react from 'eslint-plugin-react'
import eslintPluginPrettier from 'eslint-plugin-prettier/recommended'

/** @type {import('eslint').Linter.Config[]} */
export default tseslint.config(
  eslintPluginPrettier,
  ...tseslint.configs.recommended,
  {
    files: ['*.ts', '**/*.ts', '**/*.tsx', '**/*.mjs'],
    plugins: {
      react,
      '@typescript-eslint': tseslint.plugin
    },
    languageOptions: {
      parser: tseslint.parser,
      globals: globals.browser
    },
    rules: {
      'prettier/prettier': [
        'error',
        { singleQuote: true, semi: false, trailingComma: 'none', printWidth: 90 }
      ],
      quotes: [2, 'single', { avoidEscape: true }],
      '@typescript-eslint/no-empty-object-type': ['error', { allowInterfaces: 'always' }],
      '@typescript-eslint/no-empty-interface': ['off', { allowSingleExtends: true }]
    }
  },
  {
    ignores: ['./node_modules', '**/*.js']
  }
)
