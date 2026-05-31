#!/usr/bin/env node
/**
 * Post-build script: injects external resources (Bootstrap Icons, Inter font,
 * SWC importmap, SWC bundle) into the Parcel-generated index.html.
 * These resources are served by the backend (web-cache) and cannot be
 * processed by Parcel at build time.
 */
const fs = require('fs')
const path = require('path')

const distDir = path.resolve(__dirname, '..', '..', '..', '..', '..', 'work', 'frontend', 'remote.shell.react')
const indexPath = path.join(distDir, 'index.html')

if (!fs.existsSync(indexPath)) {
  console.error('ERROR: index.html not found at', indexPath)
  process.exit(1)
}

const INJECT_HEAD = `
    <!-- Bootstrap Icons (icon font only) -->
    <link href="/web-cache/cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css" rel="stylesheet">

    <!-- Google Fonts: Inter for modern UI -->
    <link href="/web-cache/fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700;800&display=swap" rel="stylesheet">
`

let html = fs.readFileSync(indexPath, 'utf-8')

// Inject before <body> (Parcel minifies away </head>)
if (html.includes('</head>')) {
  html = html.replace('</head>', INJECT_HEAD + '</head>')
} else if (html.includes('<body>')) {
  html = html.replace('<body>', INJECT_HEAD + '<body>')
} else {
  console.error('ERROR: Could not find injection point in index.html')
  process.exit(1)
}

fs.writeFileSync(indexPath, html, 'utf-8')
console.log('✓ External resources injected into index.html')
