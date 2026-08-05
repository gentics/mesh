/// <reference types='vitest/config' />
import angular from '@analogjs/vite-plugin-angular';
import { resolve } from 'node:path';
import type { PluginOption, UserConfig } from 'vite';

export function getPlugins(type: 'library' | 'angular'): PluginOption[] {
    const plugins: PluginOption[] = [];

    if (type === 'angular') {
        plugins.push(angular());
    }

    return plugins;
}

export function createProjectConfiguration(
    name: string,
    mode: 'test' | 'ci' | 'watch' = 'test',
): Partial<UserConfig> {
    return {
        cacheDir: resolve(__dirname, `node_modules/.vite/${name}`),
        resolve: {
            tsconfigPaths: true,
        },
        test: {
            name: name,
            watch: mode === 'watch',
            globals: true,
            allowOnly: mode !== 'ci' || !process.env['CI'],
            environment: 'happy-dom',
            include: [
                '**/*.spec.ts',
            ],
            exclude: [
                // Do not include the e2e tests, as these are playwright tests
                'e2e/**/*.ts',
                // Same for all cypress content
                'cypress/{fixtures,support}/**/*',
            ],
            reporters: mode === 'ci'
                ? [
                    ['minimal'],
                    ['junit', {
                        suiteName: `ui.unit.${name}`,
                        classnameTemplate: `packages/${name}/{filename}`,
                        outputFile: resolve(__dirname, `.reports/packages/${name}/VITEST-report.xml`),
                        jobSummary: false,
                        silent: true,
                    }]
                ]
                : ['default'],
            coverage: {
                enabled: true,
                reportsDirectory: resolve(__dirname, `coverage/packages/${name}`),
                provider: 'v8' as const,
                reporter: [
                    ['text-summary'],
                    ['lcovonly', { projectRoot: resolve(__dirname, `packages/${name}`) }],
                ]
            },
        },
    };
}
