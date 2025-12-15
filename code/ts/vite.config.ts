import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    host: true, // Needed for Docker
    proxy: {
      '/api': {
        target: 'http://pokerdice-jvm:8080', // Docker service name
        changeOrigin: true,
        // rewrite: (path) => path.replace(/^\/api/, '') // Only if backend doesn't expect /api
      }
    }
  },
  build: {
    sourcemap: true,
  },
  css: {
    devSourcemap: true,
  }
});