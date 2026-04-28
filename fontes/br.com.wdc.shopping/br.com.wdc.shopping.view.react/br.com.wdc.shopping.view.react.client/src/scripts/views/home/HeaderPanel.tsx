import React from 'react'
import AppBar from '@mui/material/AppBar'
import Badge from '@mui/material/Badge'
import Box from '@mui/material/Box'
import IconButton from '@mui/material/IconButton'
import Toolbar from '@mui/material/Toolbar'
import Tooltip from '@mui/material/Tooltip'
import Typography from '@mui/material/Typography'
import ShoppingCartIcon from '@mui/icons-material/ShoppingCart'
import LogoutIcon from '@mui/icons-material/Logout'
import app from '@root/App'
import { BasePanelClass } from '@root/utils/ViewUtils'

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
      <AppBar position="static" color="default" elevation={3} sx={{ bgcolor: '#333', flexShrink: 0 }}>
        <Toolbar sx={{ justifyContent: 'space-between' }}>
          <Box component="img" src="images/logo.png" alt="Shopping Triplice" sx={{ height: 36 }} />
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
            <Typography variant="body2" sx={{ color: '#fff' }}>
              Olá, <strong>{nickName}</strong>
            </Typography>
            <Tooltip title="Abrir carrinho">
              <IconButton color="inherit" onClick={this.emitOpenCart} sx={{ color: '#fff' }}>
                <Badge badgeContent={cartItemCount} color="error">
                  <ShoppingCartIcon />
                </Badge>
                <Typography variant="caption" sx={{ ml: 0.5, color: '#fff' }}>
                  Carrinho
                </Typography>
              </IconButton>
            </Tooltip>
            <Tooltip title="Sair">
              <IconButton size="small" onClick={this.emitExit} sx={{ color: '#fff' }}>
                <LogoutIcon fontSize="small" />
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
