/// <reference types='vitest' />
import { defineConfig } from 'vite';
import { createProjectConfiguration, getPlugins } from '../../vitest.project';

export default defineConfig(() => ({
    root: __dirname,
    ...createProjectConfiguration('mesh-rest-client-angular', 'ci'),
    plugins: getPlugins('angular'),
}));
