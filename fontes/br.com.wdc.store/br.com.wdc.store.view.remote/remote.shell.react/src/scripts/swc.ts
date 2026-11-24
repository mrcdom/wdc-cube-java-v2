/**
 * Re-exports of @swc-react wrappers (Spectrum Web Components for React).
 * These provide properly typed React components with event handling.
 */

// Theme tokens (color + scale definitions)
import "@spectrum-web-components/theme/theme-light.js"
import "@spectrum-web-components/theme/theme-dark.js"
import "@spectrum-web-components/theme/scale-medium.js"

export { Theme } from "@swc-react/theme"
export { Button } from "@swc-react/button"
export { ActionButton } from "@swc-react/action-button"
export { Textfield } from "@swc-react/textfield"
export { FieldLabel } from "@swc-react/field-label"
export { Divider } from "@swc-react/divider"
export { ProgressCircle } from "@swc-react/progress-circle"

export type { ThemeType } from "@swc-react/theme"
export type { TextfieldType } from "@swc-react/textfield"
