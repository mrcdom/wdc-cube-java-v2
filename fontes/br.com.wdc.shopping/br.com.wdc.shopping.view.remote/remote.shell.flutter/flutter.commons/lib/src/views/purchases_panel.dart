import 'dart:async';

import 'package:flutter/material.dart';

import '../design_tokens.dart';
import '../utils/format_utils.dart';
import 'package:remote_bridge_flutter/remote_bridge_flutter.dart';

/// Actions
const _onOpenReceipt = 1;
const _onPageChange = 2;
const _onPageSizeChange = 3;

/// PurchasesPanel — paginated order history.
class PurchasesPanel extends BaseView {
  const PurchasesPanel({super.key, required super.vsid});

  static const viewId = 'b3c4d5e6f7a8';

  @override
  State<PurchasesPanel> createState() => _PurchasesPanelState();
}

class _PurchasesPanelState extends BaseViewState<PurchasesPanel> {
  static const _fallbackItemHeight = 48.0;
  double _lastHeight = 0;
  double _measuredItemHeight = 0;
  Timer? _resizeTimer;
  bool _pendingLayoutCallback = false;
  final _firstItemKey = GlobalKey();

  @override
  void dispose() {
    _resizeTimer?.cancel();
    super.dispose();
  }

  void _onListLayout(double height) {
    if (height <= 0 || height == _lastHeight) return;
    _lastHeight = height;
    _resizeTimer?.cancel();
    _resizeTimer = Timer(const Duration(milliseconds: 150), _submitCapacity);
  }

  void _submitCapacity() {
    _measureFirstItem();
    final itemHeight = _measuredItemHeight > 0 ? _measuredItemHeight : _fallbackItemHeight;
    final capacity = (_lastHeight / itemHeight).floor().clamp(1, 100);
    final currentPageSize = (viewState['pageSize'] as num?)?.toInt() ?? 0;
    if (capacity == currentPageSize) return;
    setFormField('p.capacity', capacity);
    submit(_onPageSizeChange);
  }

  void _measureFirstItem() {
    final ctx = _firstItemKey.currentContext;
    if (ctx == null) return;
    final box = ctx.findRenderObject() as RenderBox?;
    if (box != null && box.hasSize && box.size.height > 0) {
      _measuredItemHeight = box.size.height;
    }
  }

  @override
  Widget build(BuildContext context) {
    final purchases = (viewState['purchases'] as List?)?.cast<Map<String, dynamic>>() ?? [];
    final page = (viewState['page'] as num?)?.toInt() ?? 0;
    final pageSize = (viewState['pageSize'] as num?)?.toInt().clamp(1, 100) ?? 1;
    final totalCount = (viewState['totalCount'] as num?)?.toInt() ?? 0;
    final totalPages = (totalCount / pageSize).ceil().clamp(1, 9999);

    return Container(
      decoration: const BoxDecoration(
        color: appSurface,
        border: Border(left: BorderSide(color: appBorder)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          // Header
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 16, 16, 4),
            child: Row(
              children: [
                const Icon(Icons.history, size: 18, color: appAccent),
                const SizedBox(width: 8),
                const Text('Histórico', style: TextStyle(fontSize: 14, fontWeight: FontWeight.w700)),
              ],
            ),
          ),
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16),
            child: Text('Toque para ver detalhes', style: TextStyle(fontSize: 12, color: appTextSecondary)),
          ),
          const SizedBox(height: 12),
          // List
          Expanded(
            child: LayoutBuilder(
              builder: (context, constraints) {
                if (!_pendingLayoutCallback) {
                  _pendingLayoutCallback = true;
                  WidgetsBinding.instance.addPostFrameCallback((_) {
                    _pendingLayoutCallback = false;
                    _onListLayout(constraints.maxHeight);
                  });
                }
                return ListView.builder(
                  itemCount: purchases.length,
                  itemBuilder: (context, index) {
                    final purchase = purchases[index];
                    return _PurchaseItem(
                      key: index == 0 ? _firstItemKey : null,
                      purchase: purchase,
                      onTap: () => _emitOpenReceipt(purchase),
                    );
                  },
                );
              },
            ),
          ),
          // Pagination
          if (totalCount > 0)
            Container(
              padding: const EdgeInsets.symmetric(vertical: 10),
              decoration: const BoxDecoration(
                border: Border(top: BorderSide(color: appBorder)),
              ),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  // Pill container
                  Container(
                    padding: const EdgeInsets.all(4),
                    decoration: BoxDecoration(
                      color: appBg,
                      borderRadius: BorderRadius.circular(radiusRound),
                    ),
                    child: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        // Prev button
                        GestureDetector(
                          onTap: page > 0 ? () => _emitPageChange(page - 1) : null,
                          child: Container(
                            width: 28,
                            height: 28,
                            decoration: const BoxDecoration(shape: BoxShape.circle),
                            alignment: Alignment.center,
                            child: Text('‹',
                                style: TextStyle(
                                    fontSize: 18,
                                    fontWeight: FontWeight.w300,
                                    color: page > 0 ? appTextSecondary : appTextDisabled)),
                          ),
                        ),
                        const SizedBox(width: 4),
                        // Page info pill
                        Container(
                          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
                          decoration: BoxDecoration(
                            color: appSurface,
                            borderRadius: BorderRadius.circular(radiusLg),
                            boxShadow: cardShadowSm,
                          ),
                          child: Text('${page + 1} / $totalPages',
                              style: const TextStyle(fontSize: 12, fontWeight: FontWeight.w600)),
                        ),
                        const SizedBox(width: 4),
                        // Next button
                        GestureDetector(
                          onTap: page < totalPages - 1 ? () => _emitPageChange(page + 1) : null,
                          child: Container(
                            width: 28,
                            height: 28,
                            decoration: const BoxDecoration(shape: BoxShape.circle),
                            alignment: Alignment.center,
                            child: Text('›',
                                style: TextStyle(
                                    fontSize: 18,
                                    fontWeight: FontWeight.w300,
                                    color: page < totalPages - 1
                                        ? appTextSecondary
                                        : appTextDisabled)),
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
        ],
      ),
    );
  }

  void _emitOpenReceipt(Map<String, dynamic> purchase) {
    setFormField('p.purchaseId', purchase['id']);
    submit(_onOpenReceipt);
  }

  void _emitPageChange(int page) {
    setFormField('p.page', page);
    submit(_onPageChange);
  }
}

// :: PurchaseItem

class _PurchaseItem extends StatelessWidget {
  final Map<String, dynamic> purchase;
  final VoidCallback onTap;

  const _PurchaseItem({super.key, required this.purchase, required this.onTap});

  @override
  Widget build(BuildContext context) {
    final id = purchase['id'];
    final date = (purchase['date'] as num?)?.toInt() ?? 0;
    final total = (purchase['total'] as num?)?.toDouble() ?? 0.0;
    final items = (purchase['items'] as List?)?.cast<String>() ?? [];

    final dateStr = formatDate(date);

    return InkWell(
      onTap: onTap,
      child: Container(
        margin: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
        decoration: BoxDecoration(
          border: Border.all(color: appBorder),
          borderRadius: BorderRadius.circular(radiusSm),
          color: appBg,
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text('#$id', style: const TextStyle(fontSize: 12, fontWeight: FontWeight.w600, color: appAccent)),
                Text(dateStr, style: TextStyle(fontSize: 11, color: appTextSecondary)),
              ],
            ),
            const SizedBox(height: 2),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Expanded(
                    child: Text(items.join(', '),
                        style: TextStyle(fontSize: 12, color: appTextSecondary),
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis)),
                Text(formatCurrency(total),
                    style: const TextStyle(fontSize: 12, fontWeight: FontWeight.w600)),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
