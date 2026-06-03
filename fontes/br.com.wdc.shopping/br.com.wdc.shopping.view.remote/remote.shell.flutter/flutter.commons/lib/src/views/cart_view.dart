import 'package:flutter/material.dart';

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

    const accentColor = Color(0xFF0D66D0);
    const accentLight = Color(0xFFE8F1FC);
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
            child: Container(
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
                          color: accentLight,
                          borderRadius: BorderRadius.circular(10),
                        ),
                        child: const Icon(Icons.shopping_bag_outlined, color: accentColor),
                      ),
                      const SizedBox(width: 12),
                      const Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text('Carrinho', style: TextStyle(fontSize: 16, fontWeight: FontWeight.w700)),
                          Text('Seus produtos selecionados', style: TextStyle(fontSize: 12, color: textSecondary)),
                        ],
                      ),
                    ],
                  ),
                  const SizedBox(height: 20),
                  // Error
                  if (errorMessage != null) ...[
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                      decoration: BoxDecoration(
                        color: Colors.red.shade50,
                        borderRadius: BorderRadius.circular(8),
                        border: Border.all(color: Colors.red.shade200),
                      ),
                      child: Row(
                        children: [
                          Icon(Icons.error_outline, color: Colors.red.shade700, size: 18),
                          const SizedBox(width: 8),
                          Expanded(child: Text(errorMessage, style: TextStyle(color: Colors.red.shade700, fontSize: 13))),
                        ],
                      ),
                    ),
                    const SizedBox(height: 16),
                  ],
                  // Empty state
                  if (empty) ...[
                    const SizedBox(height: 40),
                    Center(
                      child: Container(
                        width: 100,
                        height: 100,
                        decoration: const BoxDecoration(
                          color: accentLight,
                          shape: BoxShape.circle,
                        ),
                        child: const Icon(Icons.shopping_bag_outlined, size: 40, color: accentColor),
                      ),
                    ),
                    const SizedBox(height: 20),
                    const Center(child: Text('Carrinho vazio', style: TextStyle(fontSize: 16, fontWeight: FontWeight.w500))),
                    const SizedBox(height: 8),
                    const Center(child: Text('Adicione produtos para começar', style: TextStyle(fontSize: 13, color: textSecondary))),
                    const SizedBox(height: 16),
                    Center(
                      child: FilledButton.icon(
                        onPressed: _emitBack,
                        style: FilledButton.styleFrom(
                          backgroundColor: accentColor,
                          foregroundColor: Colors.white,
                          padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 14),
                          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
                          textStyle: const TextStyle(fontSize: 15, fontWeight: FontWeight.w600),
                        ),
                        icon: const Icon(Icons.grid_view, size: 18),
                        label: const Text('Ver produtos'),
                      ),
                    ),
                    const SizedBox(height: 40),
                  ],
                  // Items list
                  if (!empty) ...[
                    ...items.map((item) => _buildItem(item, accentColor, borderColor, textSecondary)),
                    // Footer total
                    Container(
                      padding: const EdgeInsets.only(top: 16),
                      margin: const EdgeInsets.only(top: 16),
                      decoration: const BoxDecoration(
                        border: Border(top: BorderSide(color: borderColor)),
                      ),
                      child: Row(
                        mainAxisAlignment: MainAxisAlignment.end,
                        children: [
                          const Text('Total: ', style: TextStyle(fontSize: 14, color: textSecondary)),
                          Text('R\$ ${total.toStringAsFixed(2)}',
                              style: const TextStyle(fontSize: 20, fontWeight: FontWeight.w800, color: accentColor)),
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
                          style: FilledButton.styleFrom(
                            backgroundColor: accentColor,
                            foregroundColor: Colors.white,
                            padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 14),
                            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
                            textStyle: const TextStyle(fontSize: 15, fontWeight: FontWeight.w600),
                          ),
                          icon: const Icon(Icons.check_circle_outline, size: 18),
                          label: const Text('Finalizar pedido'),
                        ),
                      ],
                    ),
                  ],
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildItem(Map<String, dynamic> item, Color accentColor, Color borderColor, Color textSecondary) {
    final id = (item['id'] as num?)?.toInt() ?? 0;
    final name = item['name'] as String? ?? '';
    final price = (item['price'] as num?)?.toDouble() ?? 0.0;
    final quantity = (item['quantity'] as num?)?.toInt() ?? 0;
    final subtotal = price * quantity;

    return Container(
      padding: const EdgeInsets.symmetric(vertical: 12),
      decoration: BoxDecoration(
        border: Border(bottom: BorderSide(color: borderColor)),
      ),
      child: Row(
        children: [
          Expanded(child: Text(name, style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w500))),
          // Stepper
          IconButton(
            icon: const Icon(Icons.remove, size: 16),
            onPressed: () => _emitModifyQuantity(id, quantity - 1),
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
            child: Text('R\$ ${subtotal.toStringAsFixed(2)}',
                textAlign: TextAlign.end, style: TextStyle(fontSize: 13, fontWeight: FontWeight.w700, color: accentColor)),
          ),
          IconButton(
            icon: Icon(Icons.close, size: 16, color: Colors.red.shade400),
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
