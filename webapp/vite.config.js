import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import { nodeResolve } from "@rollup/plugin-node-resolve";
import { VuetifyResolver } from "unplugin-vue-components/resolvers";
import Components from "unplugin-vue-components/vite";
import browserslistToEsbuild from "browserslist-to-esbuild";

import path from "path";

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    Components({
      resolvers: [
        // Vuetify
        VuetifyResolver(),
      ],
      // Vue version of project.
      version: 3,
    }),
    nodeResolve({
      modulePaths: [path.resolve("./node_modules")],
    }),
  ],
  build: {
    outDir: process.env.MAVEN_OUTPUT_DIR
      ? process.env.MAVEN_OUTPUT_DIR
      : "../target/dist",
    target: browserslistToEsbuild(),
  },
  preview: {
    port: 8087,
  },
  server: {
    port: 8087,
    proxy: {
      "/api": {
        // 8080 is the default port for leshan-server-demo if you change it in development phase you need to change this value too.
        target: "http://localhost:8088/m2m",
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
    },
    extensions: [".js", ".vue"],
  },
    base: "/m2m"
});
