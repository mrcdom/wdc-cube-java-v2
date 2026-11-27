import 'package:material_ui/material_ui.dart';

import '../design_tokens.dart';
import '../utils/format_utils.dart';
import 'package:remote_bridge_flutter/remote_bridge_flutter.dart';

/// Actions (the row's: CartScope.CartItemScope on the server)
const _onRemove = 1;
const _onModifyQuantity = 2;

/// CartItemView — a row of the cart: a view of its own, whose actions are the
/// row's (no product id goes back).
class CartItemView extends BaseView {
  const CartItemView({super.key, required super.vsid});

  static const viewId = 'c8a1f0e2b7d4';

  @override
  State<CartItemView> createState() => _CartItemViewState();
}

class _CartItemViewState extends BaseViewState<CartItemView> {
  @override
  Widget build(BuildContext context) {
    final name = scope['name'] as String? ?? '';
    final price = (scope['price'] as num?)?.toDouble() ?? 0.0;
    final quantity = (scope['quantity'] as num?)?.toInt() ?? 0;
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
            onPressed: quantity > 1 ? () => _emitModifyQuantity(quantity - 1) : null,
            constraints: const BoxConstraints(minWidth: 28, minHeight: 28),
            padding: EdgeInsets.zero,
          ),
          Text('$quantity', style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w700)),
          IconButton(
            icon: const Icon(Icons.add, size: 16),
            onPressed: () => _emitModifyQuantity(quantity + 1),
            constraints: const BoxConstraints(minWidth: 28, minHeight: 28),
            padding: EdgeInsets.zero,
          ),
          const SizedBox(width: 4),
          Text(formatCurrency(subtotal),
              style: const TextStyle(fontSize: 13, fontWeight: FontWeight.w700, color: appAccent)),
          IconButton(
            icon: const Icon(Icons.close, size: 16, color: appDanger),
            onPressed: _emitRemove,
            constraints: const BoxConstraints(minWidth: 28, minHeight: 28),
            padding: EdgeInsets.zero,
          ),
        ],
      ),
    );
  }

  void _emitModifyQuantity(int quantity) {
    setFormField('p.quantity', quantity);
    submit(_onModifyQuantity);
  }

  void _emitRemove() => submit(_onRemove);
}
