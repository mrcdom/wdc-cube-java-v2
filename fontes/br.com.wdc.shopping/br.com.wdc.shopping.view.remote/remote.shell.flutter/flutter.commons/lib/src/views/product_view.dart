import 'package:flutter/material.dart';

import '../bridge/view_state_coordinator.dart';
import '../widgets/html_text.dart';
import 'base_view.dart';

/// Actions
const _onOpenProducts = 1;
const _onAddToCart = 2;

/// ProductView — single product detail with add-to-cart.
class ProductView extends BaseView {
  const ProductView({super.key, required super.vsid});

  static const viewId = '48b693f67410';

  @override
  State<ProductView> createState() => _ProductViewState();
}

class _ProductViewState extends BaseViewState<ProductView> {
  int _localQuantity = 1;
  bool _quantityInitialized = false;

  @override
  Widget build(BuildContext context) {
    final state = viewState;
    final product = state['product'] as Map<String, dynamic>? ?? {};
    final name = product['name'] as String? ?? '';
    final description = product['description'] as String? ?? '';
    final price = (product['price'] as num?)?.toDouble() ?? 0.0;
    final productId = product['id'];
    final errorMessage = state['errorMessage'] as String?;

    // Sync quantity from server only once
    if (!_quantityInitialized) {
      _localQuantity = (state['quantity'] as num?)?.toInt() ?? 1;
      _quantityInitialized = true;
    }

    final imageUrl = _resolveImageUrl('./image/product/$productId.png');

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
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                // Title
                Text(name, style: const TextStyle(fontSize: 24, fontWeight: FontWeight.w700)),
                const Divider(color: accentColor, thickness: 2),
                const SizedBox(height: 16),
                // Description card (white, bordered, rounded)
                if (description.isNotEmpty)
                  Container(
                    padding: const EdgeInsets.all(20),
                    margin: const EdgeInsets.only(bottom: 20),
                    decoration: BoxDecoration(
                      color: Colors.white,
                      borderRadius: BorderRadius.circular(12),
                      border: Border.all(color: borderColor),
                    ),
                    child: HtmlText(html: description),
                  ),
                // Price + Image
                Wrap(
                  alignment: WrapAlignment.center,
                  crossAxisAlignment: WrapCrossAlignment.center,
                  spacing: 32,
                  runSpacing: 16,
                  children: [
                    // Price + quantity
                    Column(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Container(
                          padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 10),
                          decoration: BoxDecoration(
                            color: accentLight,
                            borderRadius: BorderRadius.circular(8),
                          ),
                          child: Text('R\$ ${price.toStringAsFixed(2)}',
                              style: const TextStyle(
                                  fontSize: 22,
                                  fontWeight: FontWeight.w800,
                                  color: accentColor)),
                        ),
                        const SizedBox(height: 12),
                        Row(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            const Text('Qtd:', style: TextStyle(fontSize: 14, color: textSecondary)),
                            const SizedBox(width: 10),
                            IconButton(
                              icon: const Icon(Icons.remove, size: 18),
                              onPressed: _localQuantity > 1 ? _emitDecrement : null,
                            ),
                            Text('$_localQuantity',
                                style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w700)),
                            IconButton(
                              icon: const Icon(Icons.add, size: 18),
                              onPressed: _emitIncrement,
                            ),
                          ],
                        ),
                      ],
                    ),
                    // Image
                    Container(
                      padding: const EdgeInsets.all(16),
                      decoration: BoxDecoration(
                        borderRadius: BorderRadius.circular(8),
                        gradient: const LinearGradient(
                          begin: Alignment.topCenter,
                          end: Alignment.bottomCenter,
                          colors: [Color(0xFFF8FAFC), Color(0xFFEEF2F7)],
                        ),
                      ),
                      child: SizedBox(
                        width: 160,
                        height: 160,
                        child: Image.network(
                          imageUrl,
                          fit: BoxFit.contain,
                          errorBuilder: (_, __, ___) =>
                              const Icon(Icons.image_not_supported, size: 48, color: Colors.grey),
                        ),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 24),
                // Actions (centered, like React)
                Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    TextButton.icon(
                      onPressed: _emitGoHome,
                      icon: const Icon(Icons.arrow_back, size: 18),
                      label: const Text('Voltar'),
                    ),
                    const SizedBox(width: 12),
                    FilledButton.icon(
                      onPressed: _emitAddToCart,
                      style: FilledButton.styleFrom(
                        backgroundColor: accentColor,
                        foregroundColor: Colors.white,
                        padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 14),
                        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
                        textStyle: const TextStyle(fontSize: 15, fontWeight: FontWeight.w600),
                      ),
                      icon: const Icon(Icons.add_shopping_cart, size: 18),
                      label: const Text('Adicionar ao Carrinho'),
                    ),
                  ],
                ),
                // Error
                if (errorMessage != null) ...[
                  const SizedBox(height: 16),
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
                ],
              ],
            ),
          ),
        ),
      ),
    );
  }

  void _emitIncrement() {
    setState(() => _localQuantity++);
    setFormField('quantity', _localQuantity);
  }

  void _emitDecrement() {
    if (_localQuantity > 1) {
      setState(() => _localQuantity--);
      setFormField('quantity', _localQuantity);
    }
  }

  void _emitAddToCart() {
    setFormField('p.quantity', _localQuantity);
    submit(_onAddToCart);
  }

  void _emitGoHome() {
    submit(_onOpenProducts);
  }

  static String _resolveImageUrl(String relativePath) {
    final base = ViewStateCoordinator.instance.baseWebSocketUrl.replaceFirst('ws', 'http');
    final baseUri = Uri.parse(base);
    return '${baseUri.scheme}://${baseUri.host}:${baseUri.port}/$relativePath';
  }
}
