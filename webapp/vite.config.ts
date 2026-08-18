import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import { Vuetify3Resolver } from "unplugin-vue-components/resolvers";
import Components from "unplugin-vue-components/vite";
import browserslistToEsbuild from "browserslist-to-esbuild";

import path from "path";
import { fileURLToPath } from "url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    Components({
      resolvers: [
        // Vuetify
        Vuetify3Resolver(),
      ],
      // Vue version of project.
      version: 3,
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
    // // Workarround : https://stackoverflow.com/questions/71783075/sse-doent-work-with-vue-cli-devserver-proxy
    // compress: false,
  },
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
      "@leshan-demo-servers-shared": path.join(
        __dirname,
        "../../leshan/leshan-demo-servers-shared/webapp/src"
      ),
      "vuetify/components": path.resolve(__dirname, "./node_modules/vuetify/lib/components/index.js"),
      "@mdi/js": path.resolve(__dirname, "./node_modules/@mdi/js/mdi.js"),
    },
    extensions: [".ts", ".js", ".vue"],
    dedupe: ["vue"],
  },
  base: "/",
});
