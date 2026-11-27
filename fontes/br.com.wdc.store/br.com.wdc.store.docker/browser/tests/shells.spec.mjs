import { test, expect } from "@playwright/test"

// Each web frontend through nginx: sign in, and where the screen allows it, buy a product and see the receipt.
// The remote shells' socket is routed by its session id; teavm.web's REST calls by X-Client-Id.

const RECEIPT = "Purchase completed successfully!"

test("React: signs in, buys a product and sees the receipt", async ({ page }) => {
  await page.goto("/remote.shell.react/")
  await page.getByRole("textbox", { name: "Username" }).fill("jdoe")
  await page.getByRole("textbox", { name: "Password" }).fill("jdoe")
  await page.getByRole("button", { name: "Sign in" }).click()
  await expect(page.getByText("John Doe")).toBeVisible()

  await page.getByText("Wilson Ball").first().click()
  await page.getByText("Add to Cart").click()
  await page.getByText("Checkout").click()
  await expect(page.getByText(RECEIPT)).toBeVisible()
})

// The Spectrum Web Components of the TeaVM frontends take their values and clicks from their own elements: the
// test sets them there, as a user's typing and clicking end up doing.
async function signInSwc(page, user) {
  await page.waitForFunction(() => document.querySelectorAll("sp-textfield").length >= 2)
  await page.evaluate((user) => {
    const set = (field, value) => {
      field.value = value
      field.dispatchEvent(new Event("input", { bubbles: true, composed: true }))
      field.dispatchEvent(new Event("change", { bubbles: true, composed: true }))
    }
    const [name, password] = document.querySelectorAll("sp-textfield")
    set(name, user)
    set(password, user)
    ;[...document.querySelectorAll("sp-button")].find((b) => b.textContent.trim() === "Sign in").click()
  }, user)
}

async function clickSwcButton(page, label) {
  await page.waitForFunction(
    (label) => [...document.querySelectorAll("sp-button")].some((b) => b.textContent.trim().startsWith(label)),
    label,
  )
  await page.evaluate(
    (label) => [...document.querySelectorAll("sp-button")].find((b) => b.textContent.trim().startsWith(label)).click(),
    label,
  )
}

test("TeaVM remote shell: signs in and sees Home", async ({ page }) => {
  await page.goto("/remote.shell.teavm/")
  await signInSwc(page, "admin")
  await expect(page.getByText("John Smith")).toBeVisible()
  await expect(page.getByText("Wilson Ball").first()).toBeVisible()
})

test("teavm.web: signs in, buys a product and sees the receipt", async ({ page }) => {
  await page.goto("/teavm.web/")
  await signInSwc(page, "jroe")
  await expect(page.getByText("Jane Roe")).toBeVisible()

  await page.evaluate(() => {
    const name = [...document.querySelectorAll("p")].find((p) => p.textContent.trim() === "2GB Flash Drive")
    name.parentElement.parentElement.click()
  })
  await clickSwcButton(page, "Add to Cart")
  await clickSwcButton(page, "Checkout")
  await expect(page.getByText(RECEIPT)).toBeVisible()
})
