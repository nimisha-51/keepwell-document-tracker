import { fileURLToPath } from "node:url";
import { defineConfig } from "vite";

const page = (name) => fileURLToPath(new URL(`./${name}.html`, import.meta.url));

export default defineConfig({
  build: {
    rollupOptions: {
      input: {
        index: page("index"),
        register: page("register"),
        dashboard: page("dashboard"),
        document: page("document"),
        "document-form": page("document-form"),
        renewal: page("renewal"),
        "not-found": page("not-found"),
      },
    },
  },
});
