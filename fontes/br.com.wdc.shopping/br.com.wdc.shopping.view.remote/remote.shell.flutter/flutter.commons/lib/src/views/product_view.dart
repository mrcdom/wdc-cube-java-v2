import 'package:flutter/material.dart';

import '../design_tokens.dart';
import '../utils/format_utils.dart';
import '../utils/url_utils.dart';
import '../widgets/error_banner.dart';
import '../widgets/html_text.dart';
import '../widgets/page_card.dart';
import 'package:remote_bridge_flutter/remote_bridge_flutter.dart';

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
  dynamic _lastProductId;

  @override
  Widget build(BuildContext context) {
    final state = viewState;
    final product = state['product'] as Map<String, dynamic>? ?? {};
    final name = product['name'] as String? ?? '';
    final description = product['description'] as String? ?? '';
    final price = (product['price'] as num?)?.toDouble() ?? 0.0;
    final productId = product['id'];
    final errorMessage = state['errorMessage'] as String?;

    // Re-sync quantity when product changes
    if (productId != _lastProductId) {
      _lastProductId = productId;
      _localQuantity = (state['quantity'] as num?)?.toInt() ?? 1;
    }

    return PageCard(
      useCardDecoration: false,
      children: [
                // Title
                Text(name, style: const TextStyle(fontSize: 24, fontWeight: FontWeight.w700)),
                const Divider(color: appAccent, thickness: 2),
                const SizedBox(height: 16),
                // Description card
                if (description.isNotEmpty)
                  Container(
                    padding: const EdgeInsets.all(20),
                    margin: const EdgeInsets.only(bottom: 20),
                    decoration: BoxDecoration(
                      color: appSurface,
                      borderRadius: BorderRadius.circular(radiusLg),
                      border: Border.all(color: appBorder),
                    ),
                    child: HtmlText(html: description),
                  ),
                // Price + Image
                Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  mainAxisSize: MainAxisSize.min,
                  crossAxisAlignment: CrossAxisAlignment.center,
                  children: [
                    // Price + quantity
                    Column(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Container(
                          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                          decoration: BoxDecoration(
                            color: appAccentLight,
                            borderRadius: BorderRadius.circular(radiusSm),
                          ),
                          child: Text(formatCurrency(price),
                              style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w800, color: appAccent)),
                        ),
                        const SizedBox(height: 12),
                        Row(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            const Text('Qtd:', style: TextStyle(fontSize: 14, color: appTextSecondary)),
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
                    const SizedBox(width: 16),
                    // Image
                    ConstrainedBox(
                      constraints: const BoxConstraints(maxWidth: 200, maxHeight: 200),
                      child: Container(
                        padding: const EdgeInsets.all(12),
                        decoration: BoxDecoration(
                          borderRadius: BorderRadius.circular(radiusSm),
                          gradient: imageGradient,
                        ),
                        child: AspectRatio(
                          aspectRatio: 1,
                          child: productId != null
                              ? Image.network(
                                  resolveAssetUrl('./image/product/$productId.png'),
                                  fit: BoxFit.contain,
                                  errorBuilder: (_, __, ___) => imageErrorPlaceholder(),
                                )
                              : imageErrorPlaceholder(),
                        ),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 24),
                // Actions
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
                      style: accentButtonStyle,
                      icon: const Icon(Icons.add_shopping_cart, size: 18),
                      label: const Text('Adicionar ao Carrinho'),
                    ),
                  ],
                ),
                // Error
                if (errorMessage != null) ...[
                  const SizedBox(height: 16),
                  ErrorBanner(message: errorMessage),
                ],
      ],
    );
  }

  void _emitIncrement() {
    setState(() => _localQuantity++);
  }

  void _emitDecrement() {
    if (_localQuantity > 1) {
      setState(() => _localQuantity--);
    }
  }

  void _emitAddToCart() {
    setFormField('p.quantity', _localQuantity);
    submit(_onAddToCart);
  }

  void _emitGoHome() {
    submit(_onOpenProducts);
  }
}
