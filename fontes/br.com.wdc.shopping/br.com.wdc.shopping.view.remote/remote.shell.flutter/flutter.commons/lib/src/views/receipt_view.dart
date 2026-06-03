import 'package:flutter/material.dart';

import 'base_view.dart';

/// Actions
const _onOpenProducts = 1;

/// ReceiptView — purchase receipt display.
class ReceiptView extends BaseView {
  const ReceiptView({super.key, required super.vsid});

  static const viewId = 'e8d0bd8ae3bc';

  @override
  State<ReceiptView> createState() => _ReceiptViewState();
}

class _ReceiptViewState extends BaseViewState<ReceiptView> {
  @override
  Widget build(BuildContext context) {
    final state = viewState;
    final receipt = state['receipt'] as Map<String, dynamic>? ?? {};
    final notifySuccess = state['notifySuccess'] == true;
    final items = (receipt['items'] as List?)?.cast<Map<String, dynamic>>() ?? [];
    final total = (receipt['total'] as num?)?.toDouble() ?? 0.0;
    final dateMillis = (receipt['date'] as num?)?.toInt() ?? 0;

    const monoStyle = TextStyle(fontFamily: 'Courier New', fontSize: 13);
    const monoSmall = TextStyle(fontFamily: 'Courier New', fontSize: 12);
    const monoBold = TextStyle(fontFamily: 'Courier New', fontSize: 13, fontWeight: FontWeight.w700);
    const accentColor = Color(0xFF0D66D0);
    const pageBg = Color(0xFFF4F6F9);
    const borderColor = Color(0xFFE5E5EA);
    const textSecondary = Color(0xFF6E6E73);

    return Container(
      color: pageBg,
      child: SingleChildScrollView(
        padding: const EdgeInsets.all(20),
        child: Center(
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 900),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                // Success alert
                if (notifySuccess)
                  Container(
                    margin: const EdgeInsets.only(bottom: 16),
                    padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
                    decoration: BoxDecoration(
                      color: Colors.green.shade50,
                      borderRadius: BorderRadius.circular(8),
                      border: Border.all(color: Colors.green.shade200),
                    ),
                    child: Row(
                      children: [
                        Icon(Icons.check_circle, color: Colors.green.shade700),
                        const SizedBox(width: 8),
                        Text('Compra realizada com sucesso!',
                            style: TextStyle(color: Colors.green.shade700, fontWeight: FontWeight.w500)),
                      ],
                    ),
                  ),
                // Receipt card (white, rounded, with shadow)
                Container(
                  padding: const EdgeInsets.all(28),
                  decoration: BoxDecoration(
                    color: Colors.white,
                    borderRadius: BorderRadius.circular(12),
                    border: Border.all(color: borderColor),
                    boxShadow: const [
                      BoxShadow(color: Color(0x0F000000), blurRadius: 3, offset: Offset(0, 1)),
                      BoxShadow(color: Color(0x0A000000), blurRadius: 2, offset: Offset(0, 1)),
                    ],
                  ),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      // Header
                      Row(
                        children: [
                          Container(
                            width: 40,
                            height: 40,
                            decoration: BoxDecoration(
                              color: const Color(0xFFE8F1FC),
                              borderRadius: BorderRadius.circular(10),
                            ),
                            child: const Icon(Icons.receipt_long, color: accentColor),
                          ),
                          const SizedBox(width: 12),
                          const Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text('Recibo de Compra', style: TextStyle(fontSize: 16, fontWeight: FontWeight.w700)),
                              Text('WDC Shopping', style: TextStyle(fontSize: 12, color: textSecondary)),
                            ],
                          ),
                        ],
                      ),
                      const SizedBox(height: 20),
                      // Receipt body (gray bg, rounded border, 2D)
                      Container(
                        padding: const EdgeInsets.all(20),
                        decoration: BoxDecoration(
                          color: pageBg,
                          border: Border.all(color: borderColor),
                          borderRadius: BorderRadius.circular(8),
                        ),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.stretch,
                          children: [
                            // Date row with dashed separator
                            Container(
                              padding: const EdgeInsets.only(bottom: 10),
                              decoration: const BoxDecoration(
                                border: Border(bottom: BorderSide(color: borderColor, width: 1)),
                              ),
                              child: Row(
                                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                                children: [
                                  Text('Data:', style: monoSmall.copyWith(color: textSecondary)),
                                  Text(_formatDateTime(dateMillis), style: monoBold),
                                ],
                              ),
                            ),
                            const SizedBox(height: 10),
                            // Table header
                            Container(
                              padding: const EdgeInsets.only(bottom: 6),
                              decoration: const BoxDecoration(
                                border: Border(bottom: BorderSide(color: borderColor)),
                              ),
                              child: Row(
                                children: [
                                  Expanded(child: Text('ITEM', style: monoSmall.copyWith(fontWeight: FontWeight.w700, color: textSecondary, letterSpacing: 0.5))),
                                  SizedBox(width: 80, child: Text('QTD', textAlign: TextAlign.center, style: monoSmall.copyWith(fontWeight: FontWeight.w700, color: textSecondary, letterSpacing: 0.5))),
                                  SizedBox(width: 100, child: Text('VALOR', textAlign: TextAlign.end, style: monoSmall.copyWith(fontWeight: FontWeight.w700, color: textSecondary, letterSpacing: 0.5))),
                                ],
                              ),
                            ),
                            // Items
                            ...items.map((item) {
                              final desc = item['description'] as String? ?? '';
                              final qty = (item['quantity'] as num?)?.toInt() ?? 0;
                              final value = (item['value'] as num?)?.toDouble() ?? 0.0;
                              return Container(
                                padding: const EdgeInsets.symmetric(vertical: 6),
                                decoration: const BoxDecoration(
                                  border: Border(bottom: BorderSide(color: borderColor, width: 1)),
                                ),
                                child: Row(
                                  children: [
                                    Expanded(child: Text(desc, style: monoStyle)),
                                    SizedBox(width: 80, child: Text('$qty', textAlign: TextAlign.center, style: monoStyle.copyWith(color: textSecondary))),
                                    SizedBox(width: 100, child: Text('R\$ ${value.toStringAsFixed(2)}', textAlign: TextAlign.end, style: monoBold)),
                                  ],
                                ),
                              );
                            }),
                            // Total separator (accent color line)
                            Container(
                              margin: const EdgeInsets.only(top: 12),
                              padding: const EdgeInsets.only(top: 12),
                              decoration: const BoxDecoration(
                                border: Border(top: BorderSide(color: accentColor, width: 2)),
                              ),
                              child: Row(
                                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                                children: [
                                  const Text('TOTAL:', style: TextStyle(fontFamily: 'Courier New', fontSize: 14, fontWeight: FontWeight.w700)),
                                  Text('R\$ ${total.toStringAsFixed(2)}',
                                      style: const TextStyle(fontFamily: 'Courier New', fontSize: 16, fontWeight: FontWeight.w800, color: accentColor)),
                                ],
                              ),
                            ),
                          ],
                        ),
                      ),
                      const SizedBox(height: 20),
                      // Back button
                      Align(
                        alignment: Alignment.centerLeft,
                        child: TextButton.icon(
                          onPressed: _emitOpenProducts,
                          icon: const Icon(Icons.arrow_back, size: 18),
                          label: const Text('Voltar aos produtos'),
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  void _emitOpenProducts() {
    submit(_onOpenProducts);
  }

  static String _formatDateTime(int millis) {
    if (millis == 0) return '';
    final d = DateTime.fromMillisecondsSinceEpoch(millis);
    final date = '${d.day.toString().padLeft(2, '0')}/${d.month.toString().padLeft(2, '0')}/${d.year}';
    final time = '${d.hour.toString().padLeft(2, '0')}:${d.minute.toString().padLeft(2, '0')}';
    return '$date $time';
  }
}
