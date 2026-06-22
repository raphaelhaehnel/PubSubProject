import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  base: '/app/',
  server: {
    proxy: {
      '/graph': 'http://localhost:8080',
      '/upload': 'http://localhost:8080',
      '/publish': 'http://localhost:8080',
      '/reset': 'http://localhost:8080'
    }
  }
})