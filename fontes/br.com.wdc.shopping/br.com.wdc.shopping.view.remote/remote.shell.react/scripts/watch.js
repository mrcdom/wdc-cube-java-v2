#!/usr/bin/env node
/**
 * Development watch script: starts `parcel watch` and re-runs inject-externals
 * after each rebuild. This is necessary because parcel watch rewrites dist/index.html
 * on every change, which would strip the injected external resources.
 */
const { spawn, execSync } = require('child_process')
const fs = require('fs')
const path = require('path')

const rootDir = path.resolve(__dirname, '..')
const distDir = path.resolve(rootDir, '..', '..', '..', '..', 'work', 'frontend', 'remote.shell.react')
const indexPath = path.join(distDir, 'index.html')
const injectScript = path.join(__dirname, 'inject-externals.js')

// Copy context.html once at startup
try {
  fs.mkdirSync(distDir, { recursive: true })
  fs.copyFileSync(path.join(rootDir, 'context.html'), path.join(distDir, 'context.html'))
  console.log('✓ context.html copied')
} catch (e) {
  console.warn('Warning: could not copy context.html:', e.message)
}

// Start parcel watch
const parcel = spawn('npx', ['parcel', 'watch', '--public-url', './'], {
  stdio: 'inherit',
  cwd: rootDir,
  shell: true,
})

parcel.on('exit', (code) => process.exit(code ?? 0))
process.on('SIGINT', () => {
  parcel.kill('SIGINT')
  process.exit(0)
})
process.on('SIGTERM', () => {
  parcel.kill('SIGTERM')
  process.exit(0)
})

// Re-inject after each parcel rebuild
let debounce = null

function inject() {
  try {
    const content = fs.readFileSync(indexPath, 'utf-8')
    if (!content.includes('bootstrap-icons')) {
      execSync('node ' + JSON.stringify(injectScript), { stdio: 'inherit' })
    }
  } catch (_) {
    // file may not exist yet or be mid-write — ignore
  }
}

function startWatching() {
  if (!fs.existsSync(indexPath)) {
    setTimeout(startWatching, 500)
    return
  }

  fs.watch(indexPath, (event) => {
    if (event !== 'change') return
    clearTimeout(debounce)
    debounce = setTimeout(inject, 300)
  })

  console.log('✓ Watching dist/index.html for injection...')
  inject() // inject for the initial parcel watch build
}

startWatching()
