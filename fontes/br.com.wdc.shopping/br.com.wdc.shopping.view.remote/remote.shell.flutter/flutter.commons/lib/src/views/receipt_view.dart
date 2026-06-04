import 'package:flutter/material.dart';

import '../design_tokens.dart';
import '../utils/format_utils.dart';
import '../widgets/page_card.dart';
import '../widgets/status_banner.dart';
import '../widgets/view_header.dart';
import 'package:remote_bridge_flutter/remote_bridge_flutter.dart';

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

    return PageCard(
      useCardDecoration: false,
      children: [
                // Success alert
                if (notifySuccess)
                  Padding(
                    padding: const EdgeInsets.only(bottom: 16),
                    child: StatusBanner(
                      message: 'Compra realizada com sucesso!',
                      severity: StatusSeverity.success,
                    ),
                  ),
                // Receipt card
                Container(
                  padding: const EdgeInsets.all(28),
                  decoration: cardDecoration,
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      // Header
                      const ViewHeader(
                        icon: Icons.receipt_long,
                        title: 'Recibo de Compra',
                        subtitle: 'WDC Shopping',
                      ),
                      const SizedBox(height: 20),
                      // Receipt body (gray bg)
                      Container(
                        padding: const EdgeInsets.all(20),
                        decoration: BoxDecoration(
                          color: appBg,
                          border: Border.all(color: appBorder),
                          borderRadius: BorderRadius.circular(radiusSm),
                        ),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.stretch,
                          children: [
                            // Date row
                            Container(
                              padding: const EdgeInsets.only(bottom: 10),
                              decoration: const BoxDecoration(
                                border: Border(bottom: BorderSide(color: appBorder)),
                              ),
                              child: Row(
                                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                                children: [
                                  Text('Data:', style: monoSmall.copyWith(color: appTextSecondary)),
                                  Text(formatDateTime(dateMillis), style: monoBold),
                                ],
                              ),
                            ),
                            const SizedBox(height: 10),
                            // Table header
                            Container(
                              padding: const EdgeInsets.only(bottom: 6),
                              decoration: const BoxDecoration(
                                border: Border(bottom: BorderSide(color: appBorder)),
                              ),
                              child: Row(
                                children: [
                                  Expanded(child: Text('ITEM', style: monoSmall.copyWith(fontWeight: FontWeight.w700, color: appTextSecondary, letterSpacing: 0.5))),
                                  SizedBox(width: 80, child: Text('QTD', textAlign: TextAlign.center, style: monoSmall.copyWith(fontWeight: FontWeight.w700, color: appTextSecondary, letterSpacing: 0.5))),
                                  SizedBox(width: 100, child: Text('VALOR', textAlign: TextAlign.end, style: monoSmall.copyWith(fontWeight: FontWeight.w700, color: appTextSecondary, letterSpacing: 0.5))),
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
                                  border: Border(bottom: BorderSide(color: appBorder)),
                                ),
                                child: Row(
                                  children: [
                                    Expanded(child: Text(desc, style: monoStyle)),
                                    SizedBox(width: 80, child: Text('$qty', textAlign: TextAlign.center, style: monoStyle.copyWith(color: appTextSecondary))),
                                    SizedBox(width: 100, child: Text(formatCurrency(value), textAlign: TextAlign.end, style: monoBold)),
                                  ],
                                ),
                              );
                            }),
                            // Total
                            Container(
                              margin: const EdgeInsets.only(top: 12),
                              padding: const EdgeInsets.only(top: 12),
                              decoration: const BoxDecoration(
                                border: Border(top: BorderSide(color: appAccent, width: 2)),
                              ),
                              child: Row(
                                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                                children: [
                                  const Text('TOTAL:', style: TextStyle(fontFamily: 'Courier New', fontSize: 14, fontWeight: FontWeight.w700)),
                                  Text(formatCurrency(total),
                                      style: const TextStyle(fontFamily: 'Courier New', fontSize: 16, fontWeight: FontWeight.w800, color: appAccent)),
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
    );
  }

  void _emitOpenProducts() {
    submit(_onOpenProducts);
  }
}
