import { defineConfig } from "@playwright/test"

// The web frontends through nginx (STORE_NGINX, by default the compose setup's http://localhost:18080), in Chromium.
export default defineConfig({
  testDir: "tests",
  timeout: 60_000,
  expect: { timeout: 20_000 },
  workers: 1,
  reporter: [["list"]],
  use: {
    baseURL: process.env.STORE_NGINX ?? "http://localhost:18080",
    locale: "en-US",
    trace: "retain-on-failure",
  },
})
