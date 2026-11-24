import 'package:flutter/material.dart';

import '../design_tokens.dart';
import '../utils/format_utils.dart';
import '../widgets/error_banner.dart';
import '../widgets/page_card.dart';
import '../widgets/view_header.dart';
import 'package:remote_bridge_flutter/remote_bridge_flutter.dart';

/// Actions
const _onBuy = 1;
const _onBack = 3;

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
    // The rows are views of their own (CartItemView), referred to by id in the cart's order
    final rows = (state['rowsViewIds'] as List?)?.cast<String>() ?? [];
    final errorMessage = state['errorMessage'] as String?;
    final empty = rows.isEmpty;
    final total = (state['total'] as num?)?.toDouble() ?? 0.0;

    return PageCard(
      children: [
        // Header
        const ViewHeader(
          icon: Icons.shopping_bag_outlined,
          title: 'Cart',
          subtitle: 'Your selected products',
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
          const Center(child: Text('Your cart is empty', style: TextStyle(fontSize: 16, fontWeight: FontWeight.w500))),
          const SizedBox(height: 8),
          const Center(child: Text('Add products to get started', style: TextStyle(fontSize: 13, color: appTextSecondary))),
          const SizedBox(height: 16),
          Center(
            child: FilledButton.icon(
              onPressed: _emitBack,
              style: accentButtonStyle,
              icon: const Icon(Icons.grid_view, size: 18),
              label: const Text('View products'),
            ),
          ),
          const SizedBox(height: 40),
        ],
        // Items list
        if (!empty) ...[
          ...rows.map(slot),
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
          Wrap(
            alignment: WrapAlignment.center,
            spacing: 8,
            runSpacing: 8,
            children: [
              TextButton.icon(
                onPressed: _emitBack,
                icon: const Icon(Icons.arrow_back, size: 18),
                label: const Text('Continue shopping'),
              ),
              FilledButton.icon(
                onPressed: _emitBuy,
                style: accentButtonStyle,
                icon: const Icon(Icons.check_circle_outline, size: 18),
                label: const Text('Checkout'),
              ),
            ],
          ),
        ],
      ],
    );
  }

  void _emitBack() => submit(_onBack);
  void _emitBuy() => submit(_onBuy);
}
