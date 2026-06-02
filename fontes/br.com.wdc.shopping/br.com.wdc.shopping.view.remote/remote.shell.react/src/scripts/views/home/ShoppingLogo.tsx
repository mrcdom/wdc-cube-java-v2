import React from 'react'

type ShoppingLogoProps = {
  height?: number
}

export default function ShoppingLogo({ height = 36 }: ShoppingLogoProps) {
  const w = height * 5.5
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      viewBox="0 0 220 40"
      width={w}
      height={height}
      role="img"
      aria-label="WDC Shopping"
    >
      {/* Price tag icon */}
      <g transform="translate(-2, -2) scale(1.3)">
        {/* Tag body - rotated */}
        <path d="M4 18 L14 4 L28 4 L28 18 L18 32 Z" fill="#ff9800" stroke="#ffb74d" strokeWidth="1" />
        {/* Tag highlight */}
        <path d="M14 4 L28 4 L28 12 L8 12 Z" fill="#ffc107" opacity="0.5" />
        {/* Tag hole */}
        <circle cx="23" cy="10" r="2.5" fill="#1976d2" />
        <circle cx="23" cy="10" r="1.2" fill="#fff" />
        {/* Dollar/currency symbol */}
        <text x="16" y="24" fontFamily="Arial" fontWeight="bold" fontSize="11" fill="#fff">
          $
        </text>
      </g>

      {/* "Shopping" main text */}
      <text
        x="40"
        y="20"
        fontFamily="Verdana, Geneva, Tahoma, sans-serif"
        fontWeight="bold"
        fontSize="20"
        fill="#ffffff"
        letterSpacing="1"
      >
        Shopping
      </text>

      {/* "by WeDoCode" subtle branding - below Shopping */}
      <text
        x="40"
        y="34"
        fontFamily="Verdana, Geneva, Tahoma, sans-serif"
        fontWeight="normal"
        fontSize="8"
        fill="rgba(255,255,255,0.5)"
        letterSpacing="0.5"
      >
        by
      </text>
      <text
        x="52"
        y="34"
        fontFamily="Verdana, Geneva, Tahoma, sans-serif"
        fontWeight="bold"
        fontSize="8"
        fill="rgba(255,255,255,0.65)"
        letterSpacing="0.5"
      >
        WeDoCode
      </text>
    </svg>
  )
}
