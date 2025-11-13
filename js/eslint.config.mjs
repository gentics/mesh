/* eslint-disable import-x/no-named-as-default-member */
import js from '@eslint/js';
import nxPlugin from '@nx/eslint-plugin';
import stylistic from '@stylistic/eslint-plugin';
import importPlugin from 'eslint-plugin-import-x';
import { jsdoc } from 'eslint-plugin-jsdoc';
import globals from 'globals';
import tseslint from 'typescript-eslint';
import * as tsParser from '@typescript-eslint/parser';
import { defineConfig } from 'eslint/config';

export default defineConfig([
    js.configs.recommended,
    tseslint.configs.strictTypeChecked,
    stylistic.configs.recommended,
    importPlugin.flatConfigs.recommended,
    importPlugin.flatConfigs.typescript,
    jsdoc({
        config: 'flat/recommended',
    }),
    {
        plugins: {
            '@nx': nxPlugin,
        },
    },
    {
        files: ['**/*.mjs', '**/*.js', '**/*.ts'],

        languageOptions: {
            parser: tsParser,
            ecmaVersion: 'latest',
            sourceType: 'module',
            parserOptions: {
                projectService: true,
            },
            globals: {
                ...globals.node,
            },
        },

        rules: {
            '@stylistic/arrow-parens': ['warn', 'always'],
            '@stylistic/brace-style': ['error', '1tbs', {
                allowSingleLine: true,
            }],
            '@stylistic/indent': ['warn', 4],
            '@stylistic/member-delimiter-style': ['error', {
                multiline: {
                    delimiter: 'semi',
                    requireLast: true,
                },
                singleline: {
                    delimiter: 'semi',
                    requireLast: false,
                },
                multilineDetection: 'brackets',
            }],
            '@stylistic/padded-blocks': 'off',
            '@stylistic/quotes': ['error', 'single', {
                avoidEscape: true,
            }],
            '@stylistic/semi': ['error', 'always'],

            '@typescript-eslint/adjacent-overload-signatures': 'error',
            '@typescript-eslint/array-type': 'off',
            '@typescript-eslint/await-thenable': 'error',
            '@typescript-eslint/ban-ts-comment': 'error',
            '@typescript-eslint/consistent-type-assertions': 'off',
            '@typescript-eslint/dot-notation': 'off',

            '@typescript-eslint/explicit-member-accessibility': ['off', {
                accessibility: 'explicit',
            }],

            '@typescript-eslint/explicit-module-boundary-types': 'warn',

            '@typescript-eslint/member-ordering': 'off',

            '@typescript-eslint/naming-convention': ['warn', {
                selector: ['variable', 'parameter'],
                modifiers: ['unused'],
                format: ['camelCase'],

                custom: {
                    match: true,
                    regex: '^_?.*$',
                },

                leadingUnderscore: 'require',
            }, {
                selector: ['enumMember'],
                format: ['UPPER_CASE'],
            }, {
                selector: ['variable'],
                modifiers: ['global', 'const'],
                types: ['array', 'boolean', 'number', 'string'],
                format: ['UPPER_CASE'],
            }, {
                selector: ['classProperty'],
                modifiers: ['readonly'],
                format: ['UPPER_CASE', 'PascalCase'],
            }, {
                selector: ['typeLike'],
                format: ['PascalCase'],
            }, {
                selector: ['variable'],
                modifiers: ['const'],
                format: ['UPPER_CASE', 'camelCase'],
            }, {
                selector: ['variableLike', 'memberLike', 'property', 'method'],
                format: ['camelCase'],
            }, {
                selector: ['parameter'],
                format: null,

                filter: {
                    regex: '^_$',
                    match: true,
                },
            }, {
                selector: [
                    'classProperty',
                    'objectLiteralProperty',
                    'typeProperty',
                    'classMethod',
                    'objectLiteralMethod',
                    'typeMethod',
                    'accessor',
                    'enumMember',
                ],

                format: null,
                modifiers: ['requiresQuotes'],
            }],

            '@typescript-eslint/no-array-constructor': 'error',
            '@typescript-eslint/no-empty-object-type': 'off',
            '@typescript-eslint/no-empty-function': 'off',
            '@typescript-eslint/no-empty-interface': 'off',
            '@typescript-eslint/no-explicit-any': 'off',
            '@typescript-eslint/no-extra-non-null-assertion': 'error',
            '@typescript-eslint/no-floating-promises': 'off',
            '@typescript-eslint/no-for-in-array': 'error',
            '@typescript-eslint/no-implied-eval': 'error',

            '@typescript-eslint/no-inferrable-types': ['error', {
                ignoreParameters: true,
            }],

            '@typescript-eslint/no-misused-new': 'error',
            '@typescript-eslint/no-misused-promises': 'error',
            '@typescript-eslint/no-namespace': 'error',
            '@typescript-eslint/no-non-null-asserted-optional-chain': 'error',
            '@typescript-eslint/no-non-null-assertion': 'off',
            '@typescript-eslint/no-parameter-properties': 'off',
            '@typescript-eslint/no-this-alias': 'error',
            '@typescript-eslint/no-unnecessary-type-assertion': 'error',
            '@typescript-eslint/no-unsafe-assignment': 'off',
            '@typescript-eslint/no-unsafe-argument': 'off',
            '@typescript-eslint/no-unsafe-call': 'error',
            '@typescript-eslint/no-unsafe-member-access': 'off',
            '@typescript-eslint/no-unsafe-return': 'off',
            '@typescript-eslint/no-unused-expressions': 'error',

            '@typescript-eslint/no-unused-vars': ['warn', {
                varsIgnorePattern: '^_',
                argsIgnorePattern: '^_',
                destructuredArrayIgnorePattern: '^_',
                caughtErrors: 'none',
            }],

            '@typescript-eslint/no-use-before-define': 'warn',
            '@typescript-eslint/no-var-requires': 'off',
            '@typescript-eslint/prefer-as-const': 'error',
            '@typescript-eslint/prefer-for-of': 'error',
            '@typescript-eslint/prefer-function-type': 'error',
            '@typescript-eslint/prefer-namespace-keyword': 'error',
            '@typescript-eslint/prefer-regexp-exec': 'error',
            '@typescript-eslint/require-await': 'error',
            '@typescript-eslint/restrict-plus-operands': 'error',
            '@typescript-eslint/restrict-template-expressions': 'off',

            '@typescript-eslint/triple-slash-reference': ['error', {
                path: 'always',
                types: 'prefer-import',
                lib: 'always',
            }],

            '@typescript-eslint/unbound-method': ['warn', {
                ignoreStatic: true,
            }],

            '@typescript-eslint/unified-signatures': 'error',

            'import-x/no-deprecated': 'warn',
            'import-x/order': 'warn',
            'import-x/no-nodejs-modules': 'error',
            'import-x/no-mutable-exports': 'error',
            'import-x/no-empty-named-blocks': 'warn',
            'import-x/no-deprecated': 'off',

            'jsdoc/check-alignment': 'error',
            'jsdoc/check-indentation': 'error',
            'jsdoc/check-param-names': 'error',
            'jsdoc/check-property-names': 'error',
            'jsdoc/no-types': 'error',

            'arrow-parens': ['off', 'always'],
            'comma-dangle': ['error', {
                imports: 'always-multiline',
                objects: 'always-multiline',
                arrays: 'always-multiline',
                functions: 'always-multiline',
            }],

            complexity: 'off',
            'constructor-super': 'error',
            eqeqeq: ['error', 'smart'],
            'guard-for-in': 'error',

            'id-blacklist': [
                'error',
                'any',
                'Number',
                'number',
                'String',
                'string',
                'Boolean',
                'boolean',
                'Undefined',
                'undefined',
            ],

            indent: 'off',
            'id-match': 'error',

            'linebreak-style': ['error', 'unix'],
            'max-classes-per-file': 'off',

            'max-len': ['error', {
                code: 160,
                ignoreRegExpLiterals: true,
                ignoreTemplateLiterals: true,
            }],

            'new-parens': 'error',
            'no-array-constructor': 'off',
            'no-bitwise': 'error',
            'no-caller': 'error',
            'no-cond-assign': 'error',

            'no-console': ['error', {
                allow: [
                    'log',
                    'warn',
                    'dir',
                    'timeLog',
                    'assert',
                    'clear',
                    'count',
                    'countReset',
                    'group',
                    'groupEnd',
                    'table',
                    'dirxml',
                    'error',
                    'groupCollapsed',
                    'Console',
                    'profile',
                    'profileEnd',
                    'timeStamp',
                    'context',
                ],
            }],

            'no-debugger': 'error',
            'no-empty': 'off',
            'no-empty-function': 'off',
            'no-eval': 'error',
            'no-extra-semi': 'off',
            'no-fallthrough': 'error',
            'no-invalid-this': 'off',
            'no-multiple-empty-lines': 'off',
            'no-new-wrappers': 'error',
            'no-prototype-builtins': 'off',
            'no-restricted-imports': ['error', 'rxjs/Rx'],

            'no-shadow': ['off', {
                hoist: 'all',
            }],

            'no-throw-literal': 'error',
            'no-trailing-spaces': 'error',
            'no-undef-init': 'error',
            'no-underscore-dangle': 'error',
            'no-unsafe-finally': 'error',
            'no-unused-labels': 'error',
            'no-unused-vars': 'off',
            'no-var': 'error',
            'object-shorthand': 'off',
            'one-var': ['error', 'never'],
            'quote-props': ['error', 'as-needed'],
            radix: 'error',
            'require-await': 'off',

            'spaced-comment': ['error', 'always', {
                markers: ['/'],
            }],

            'use-isnan': 'error',
            'valid-typeof': 'off',
        },
    }, {
        files: ['**/*.spec.ts'],

        rules: {
            '@typescript-eslint/no-floating-promises': 'off',
            '@typescript-eslint/no-unsafe-argument': 'off',
            '@typescript-eslint/unbound-method': 'off',
            '@typescript-eslint/no-use-before-define': 'off',
            '@typescript-eslint/no-unsafe-call': 'off',
            '@typescript-eslint/no-unsafe-member-access': 'off',
            '@typescript-eslint/no-unused-vars': 'off',
            '@typescript-eslint/no-unsafe-return': 'off',
            '@typescript-eslint/no-unsafe-assignment': 'off',
            '@typescript-eslint/no-non-null-assertion': 'off',
            '@typescript-eslint/member-ordering': 'off',
        },
    },
]);
