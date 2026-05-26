import React from 'react'
import AppBar from '@mui/material/AppBar'
import Badge from '@mui/material/Badge'
import Box from '@mui/material/Box'
import IconButton from '@mui/material/IconButton'
import Toolbar from '@mui/material/Toolbar'
import Tooltip from '@mui/material/Tooltip'
import Typography from '@mui/material/Typography'
import ShoppingCartIcon from '@mui/icons-material/ShoppingCart'
import PowerSettingsNewIcon from '@mui/icons-material/PowerSettingsNew'
import app from '@root/App'
import { BasePanelClass } from '@root/utils/ViewUtils'
import ShoppingLogo from './ShoppingLogo'

// :: Actions

const ON_EXIT = 1
const ON_OPEN_CART = 2

// :: Panel

type HeaderPanelProps = {
  vsid: string
  nickName: string
  cartItemCount: number
}

class HeaderPanelClass extends BasePanelClass<HeaderPanelProps> {
  vsid!: string

  override render({ vsid, nickName, cartItemCount }: HeaderPanelProps) {
    this.vsid = vsid

    return (
      <AppBar position="static" color="primary" elevation={2} sx={{ flexShrink: 0 }}>
        <Toolbar>
          {/* Left: Sair + Olá */}
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, flex: 1 }}>
            <Tooltip title="Sair">
              <IconButton size="small" onClick={this.emitExit} sx={{ color: '#fff' }}>
                <PowerSettingsNewIcon fontSize="small" />
              </IconButton>
            </Tooltip>
            <Typography variant="body2" sx={{ color: '#fff' }}>
              Olá, {nickName}
            </Typography>
          </Box>

          {/* Center: Logo */}
          <ShoppingLogo height={32} />

          {/* Right: Carrinho */}
          <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'flex-end', flex: 1 }}>
            <Tooltip title="Abrir carrinho">
              <IconButton color="inherit" onClick={this.emitOpenCart} sx={{ color: '#fff' }}>
                <Badge badgeContent={cartItemCount} color="error">
                  <ShoppingCartIcon />
                </Badge>
                <Typography variant="body2" sx={{ ml: 0.5, color: '#fff' }}>
                  Carrinho
                </Typography>
              </IconButton>
            </Tooltip>
          </Box>
        </Toolbar>
      </AppBar>
    )
  }

  // :: Emissors

  readonly emitOpenCart = () => {
    const { vsid } = this
    app.submit(vsid, ON_OPEN_CART)
  }

  readonly emitExit = () => {
    const { vsid } = this
    app.submit(vsid, ON_EXIT)
  }
}

export default BasePanelClass.FC(HeaderPanelClass)
