import React from 'react'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Card from '@mui/material/Card'
import CardContent from '@mui/material/CardContent'
import Divider from '@mui/material/Divider'
import Table from '@mui/material/Table'
import TableBody from '@mui/material/TableBody'
import TableCell from '@mui/material/TableCell'
import TableHead from '@mui/material/TableHead'
import TableRow from '@mui/material/TableRow'
import Typography from '@mui/material/Typography'
import ArrowBackIcon from '@mui/icons-material/ArrowBack'
import CheckCircleOutlinedIcon from '@mui/icons-material/CheckCircleOutlined'
import app, { type ViewProps } from '@root/App'
import { BaseViewClass } from '@root/utils/ViewUtils'
import * as NumberUtils from '@root/utils/NumberUtils'

// :: Actions

const ON_OPEN_PRODUCTS = 1

// :: Types

type ReceiptItem = {
  description: string
  value: number
  quantity: number
}

type ReceiptForm = {
  date: number
  items: ReceiptItem[]
  total: number
}

// :: View

export type ReceiptViewState = {
  receipt: ReceiptForm
  notifySuccess?: boolean
}

class ReceiptViewClass extends BaseViewClass<ViewProps, ReceiptViewState> {
  // :: Renderes

  override render({ className }: ViewProps) {
    const { state } = this
    const reciboItems = state.receipt.items

    return (
      <Card className={className} elevation={3} sx={{ maxWidth: 900, mx: 'auto', my: 3 }}>
        <CardContent>
          {state.notifySuccess && (
            <Alert icon={<CheckCircleOutlinedIcon fontSize="inherit" />} severity="success" sx={{ mb: 2 }}>
              <Typography variant="subtitle1" sx={{ fontWeight: 'bold' }}>
                COMPRA EFETUADA COM SUCESSO
              </Typography>
            </Alert>
          )}

          <Typography variant="h6" gutterBottom>
            IMPRIMA SEU RECIBO:
          </Typography>

          <Box
            sx={{
              border: '1px solid',
              borderColor: 'grey.500',
              borderRadius: 1,
              p: 2,
              fontFamily: '"Courier New", Courier, monospace',
            }}
          >
            <Typography variant="subtitle2" sx={{ fontWeight: 'bold' }}>
              STELA SHOPPING - SUA COMPRA CERTA NA INTERNET
            </Typography>
            <Typography variant="subtitle2" gutterBottom>
              Recibo de compra
            </Typography>
            <Divider sx={{ my: 1, borderStyle: 'dashed' }} />
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell sx={{ fontFamily: 'inherit', fontWeight: 'bold' }}>ITEM</TableCell>
                  <TableCell align="right" sx={{ fontFamily: 'inherit', fontWeight: 'bold' }}>
                    VALOR UNITÁRIO
                  </TableCell>
                  <TableCell align="center" sx={{ fontFamily: 'inherit', fontWeight: 'bold' }}>
                    QUANTIDADE
                  </TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {reciboItems.map((item, idx) => (
                  <TableRow key={idx}>
                    <TableCell sx={{ fontFamily: 'inherit' }}>{item.description}</TableCell>
                    <TableCell align="right" sx={{ fontFamily: 'inherit' }}>
                      R$ {NumberUtils.format(item.value)}
                    </TableCell>
                    <TableCell align="center" sx={{ fontFamily: 'inherit' }}>
                      {NumberUtils.format(item.quantity, 0)}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
            <Divider sx={{ my: 1.5 }} />
            <Box sx={{ display: 'flex', justifyContent: 'flex-end' }}>
              <Typography variant="body2" sx={{ fontFamily: 'inherit', fontWeight: 'bold' }}>
                VALOR TOTAL: R$ {NumberUtils.format(state.receipt.total)}
              </Typography>
            </Box>
          </Box>

          <Box sx={{ mt: 3 }}>
            <Button variant="outlined" startIcon={<ArrowBackIcon />} onClick={this.emitOpenProducts}>
              Voltar
            </Button>
          </Box>
        </CardContent>
      </Card>
    )
  }

  // :: Emissors

  readonly emitOpenProducts = () => {
    const { vsid } = this
    app.submit(vsid, ON_OPEN_PRODUCTS)
  }
}

export default BaseViewClass.FC(ReceiptViewClass, 'e8d0bd8ae3bc')
