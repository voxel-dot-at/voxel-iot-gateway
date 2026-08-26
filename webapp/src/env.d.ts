declare module "*.vue" {
  import type { DefineComponent } from "vue";
  const component: DefineComponent<{}, {}, any>;
  export default component;
}

declare module "vue-sse" {
  export class SSEManager {
    create(options: Record<string, unknown>): any;
  }
}
