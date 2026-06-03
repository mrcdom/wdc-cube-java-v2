import 'package:flutter/material.dart';

import '../design_tokens.dart';
import '../utils/format_utils.dart';
import '../widgets/error_banner.dart';
import '../widgets/page_card.dart';
import '../widgets/view_header.dart';
import 'base_view.dart';

/// Actions
const _onBuy = 1;
const _onRemove = 2;
const _onBack = 3;
const _onModifyQuantity = 4;

/// CartView — shopping cart with items, quantity controls, and checkout.
class CartView extends BaseView {
  const CartView({super.key, required super.vsid});

  static const viewId = '7eb485e5f843';

  @override
  State<CartView> createState() => _CartViewState();
}

class _CartViewState extends BaseViewState<CartView> {
  @override
  Widget build(BuildContext context) {
    final state = viewState;
    final items = (state['items'] as List?)?.cast<Map<String, dynamic>>() ?? [];
    final errorMessage = state['errorMessage'] as String?;
    final empty = items.isEmpty;
    final total = items.fold<double>(
        0, (sum, item) => sum + ((item['price'] as num?)?.toDouble() ?? 0) * ((item['quantity'] as num?)?.toInt() ?? 0));

    return PageCard(
      children: [
        // Header
        const ViewHeader(
          icon: Icons.shopping_bag_outlined,
          title: 'Carrinho',
          subtitle: 'Seus produtos selecionados',
        ),
        const SizedBox(height: 20),
        // Error
        if (errorMessage != null) ...[
          ErrorBanner(message: errorMessage),
          const SizedBox(height: 16),
        ],
        // Empty state
        if (empty) ...[
          const SizedBox(height: 40),
          Center(
            child: Container(
              width: 100,
              height: 100,
              decoration: const BoxDecoration(color: appAccentLight, shape: BoxShape.circle),
              child: const Icon(Icons.shopping_bag_outlined, size: 40, color: appAccent),
            ),
          ),
          const SizedBox(height: 20),
          const Center(child: Text('Carrinho vazio', style: TextStyle(fontSize: 16, fontWeight: FontWeight.w500))),
          const SizedBox(height: 8),
          const Center(child: Text('Adicione produtos para começar', style: TextStyle(fontSize: 13, color: appTextSecondary))),
          const SizedBox(height: 16),
          Center(
            child: FilledButton.icon(
              onPressed: _emitBack,
              style: accentButtonStyle,
              icon: const Icon(Icons.grid_view, size: 18),
              label: const Text('Ver produtos'),
            ),
          ),
          const SizedBox(height: 40),
        ],
        // Items list
        if (!empty) ...[
          ...items.map(_buildItem),
          // Footer total
          Container(
            padding: const EdgeInsets.only(top: 16),
            margin: const EdgeInsets.only(top: 16),
            decoration: const BoxDecoration(
              border: Border(top: BorderSide(color: appBorder)),
            ),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.end,
              children: [
                const Text('Total: ', style: TextStyle(fontSize: 14, color: appTextSecondary)),
                Text(formatCurrency(total),
                    style: const TextStyle(fontSize: 20, fontWeight: FontWeight.w800, color: appAccent)),
              ],
            ),
          ),
          const SizedBox(height: 16),
          // Actions
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              TextButton.icon(
                onPressed: _emitBack,
                icon: const Icon(Icons.arrow_back, size: 18),
                label: const Text('Continuar comprando'),
              ),
              FilledButton.icon(
                onPressed: _emitBuy,
                style: accentButtonStyle,
                icon: const Icon(Icons.check_circle_outline, size: 18),
                label: const Text('Finalizar pedido'),
              ),
            ],
          ),
        ],
      ],
    );
  }

  Widget _buildItem(Map<String, dynamic> item) {
    final id = (item['id'] as num?)?.toInt() ?? 0;
    final name = item['name'] as String? ?? '';
    final price = (item['price'] as num?)?.toDouble() ?? 0.0;
    final quantity = (item['quantity'] as num?)?.toInt() ?? 0;
    final subtotal = price * quantity;

    return Container(
      padding: const EdgeInsets.symmetric(vertical: 12),
      decoration: const BoxDecoration(
        border: Border(bottom: BorderSide(color: appBorder)),
      ),
      child: Row(
        children: [
          Expanded(child: Text(name, style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w500))),
          IconButton(
            icon: const Icon(Icons.remove, size: 16),
            onPressed: quantity > 1 ? () => _emitModifyQuantity(id, quantity - 1) : null,
            constraints: const BoxConstraints(minWidth: 32, minHeight: 32),
          ),
          Text('$quantity', style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w700)),
          IconButton(
            icon: const Icon(Icons.add, size: 16),
            onPressed: () => _emitModifyQuantity(id, quantity + 1),
            constraints: const BoxConstraints(minWidth: 32, minHeight: 32),
          ),
          const SizedBox(width: 8),
          SizedBox(
            width: 90,
            child: Text(formatCurrency(subtotal),
                textAlign: TextAlign.end, style: const TextStyle(fontSize: 13, fontWeight: FontWeight.w700, color: appAccent)),
          ),
          IconButton(
            icon: const Icon(Icons.close, size: 16, color: appDanger),
            onPressed: () => _emitRemove(id),
            constraints: const BoxConstraints(minWidth: 32, minHeight: 32),
          ),
        ],
      ),
    );
  }

  void _emitBack() => submit(_onBack);
  void _emitBuy() => submit(_onBuy);

  void _emitModifyQuantity(int productId, int quantity) {
    setFormField('p.productId', productId);
    setFormField('p.quantity', quantity);
    submit(_onModifyQuantity);
  }

  void _emitRemove(int productId) {
    setFormField('p.productId', productId);
    submit(_onRemove);
  }
}
