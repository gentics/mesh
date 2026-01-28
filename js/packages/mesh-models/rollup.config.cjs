const { withNx } = require('@nx/rollup/with-nx');

module.exports = withNx(
  {
    main: './src/public-api.ts',
    outputPath: '../../dist/packages/mesh-models',
    tsConfig: './tsconfig.lib.json',
    compiler: 'swc',
    format: ['cjs', 'esm'],
    assets: [{ input: '{projectRoot}', output: '.', glob: '*.md' }],
  },
  {
    // Provide additional rollup configuration here. See: https://rollupjs.org/configuration-options
    // e.g.
    // output: { sourcemap: true },
  }
);
