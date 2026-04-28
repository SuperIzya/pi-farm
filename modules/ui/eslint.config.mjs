import { defineConfig, globalIgnores } from 'eslint/config'
import globals from 'globals'
import tseslint from 'typescript-eslint'
import prettierConfig from 'eslint-config-prettier'
import prettierPlugin from 'eslint-plugin-prettier'
import stylistic from '@stylistic/eslint-plugin'

const commonRules = {
  'no-unused-expressions': 'off',
  'no-useless-constructor': 'off',
  'no-empty': 'off',

  '@typescript-eslint/restrict-template-expressions': 'off',
  '@typescript-eslint/ban-ts-comment': 'off',
  '@typescript-eslint/no-explicit-any': 'off',
  '@typescript-eslint/no-unnecessary-condition': 'off',
  '@typescript-eslint/no-unused-vars': [
    'error',
    {
      varsIgnorePattern: '^_.*',
      argsIgnorePattern: '^_.*'
    }
  ],
  '@typescript-eslint/no-namespace': 'off',

  '@stylistic/type-generic-spacing': ['error'],
  '@stylistic/member-delimiter-style': [
    'error',
    {
      multiline: { delimiter: 'none', requireLast: true },
      singleline: { delimiter: 'comma', requireLast: false }
    }
  ],

  'prettier/prettier': [
    'error',
    {
      tabWidth: 2,
      useTabs: false,
      semi: false,
      endOfLine: 'lf',
      bracketSpacing: true,
      trailingComma: 'none',
      singleQuote: true,
      jsxSingleQuote: true,
      experimentalOperatorPosition: 'start',
      experimentalTernaries: false,
      printWidth: 100,
      quotes: 'single',
      quoteProps: 'as-needed',
      objectWrap: 'preserve',
      arrowParens: 'avoid'
    }
  ]
}

export default defineConfig([
  globalIgnores([
    '**/build',
    '**/node_modules',
    '**/*.js',
    '**/*.js.map',
    '**/*.d.ts'
  ]),
  tseslint.configs.recommended,
  prettierConfig,
  {
    files: ['**/*.ts', '**/*.tsx'],
    plugins: {
      prettier: prettierPlugin,
      '@stylistic': stylistic
    },

    languageOptions: {
      globals: {
        ...globals.browser,
        ...globals.commonjs,
        ...globals.node
      },
      parserOptions: {
        projectService: true
      }
    },

    rules: commonRules
  },
  {
    files: ['eslint.config.mjs'],
    plugins: {
      prettier: prettierPlugin,
      '@stylistic': stylistic
    },

    languageOptions: {
      globals: globals.node
    },

    rules: commonRules
  }
])
