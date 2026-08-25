import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import { Vuetify3Resolver } from "unplugin-vue-components/resolvers";
import Components from "unplugin-vue-components/vite";
import browserslistToEsbuild from "browserslist-to-esbuild";
import vueDevTools from 'vite-plugin-vue-devtools'

import path from "path";
import { fileURLToPath } from "url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    vueDevTools(),
    Components({
      resolvers: [
        // Vuetify
        Vuetify3Resolver(),
      ]
    }),
  ],
  build: {
    outDir: process.env.MAVEN_OUTPUT_DIR
      ? process.env.MAVEN_OUTPUT_DIR
      : "../target/dist",
    target: browserslistToEsbuild(),
    rollupOptions: {
      output: {
        manualChunks: (id) => {
          if (id.includes("vuetify")) {
            return "vuetify";
          }
          if (id.includes("node_modules")) {
            return "vendor";
          }
        },
      },
    },
  },
  preview: {
    port: 8087,
    proxy: {
      "/api": {
        // 8080 is the default port for leshan-server-demo if you change it in development phase you need to change this value too.
        target: "http://localhost:8088",
        ws: true,
        changeOrigin: true,
      },
    },
  },
  server: {
    port: 8088,
    proxy: {
      "/api": {
        // 8080 is the default port for leshan-server-demo if you change it in development phase you need to change this value too.
        target: "http://localhost:8088",
        ws: true,
        changeOrigin: true,
      },
    },
  },
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
      "vuetify/components": path.resolve(__dirname, "./node_modules/vuetify/lib/components/index.js"),
      "@mdi/js": path.resolve(__dirname, "./node_modules/@mdi/js/mdi.js"),
    },
    extensions: [".ts", ".js", ".vue"],
    dedupe: ["vue"],
  },
  base: "/",
});
