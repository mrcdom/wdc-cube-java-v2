import 'package:flutter/material.dart';

import '../design_tokens.dart';
import '../utils/format_utils.dart';
import '../utils/url_utils.dart';
import '../widgets/hover_card.dart';
import 'base_view.dart';

/// Actions
const _onOpenProduct = 1;

/// ProductsPanel — grid of product cards.
class ProductsPanel extends BaseView {
  const ProductsPanel({super.key, required super.vsid});

  static const viewId = 'a1b2c3d4e5f6';

  @override
  State<ProductsPanel> createState() => _ProductsPanelState();
}

class _ProductsPanelState extends BaseViewState<ProductsPanel> {
  @override
  Widget build(BuildContext context) {
    final products = (viewState['products'] as List?)?.cast<Map<String, dynamic>>() ?? [];

    if (products.isEmpty) {
      return const Center(child: Text('Nenhum produto disponível'));
    }

    return LayoutBuilder(builder: (context, constraints) {
      final crossAxisCount = constraints.maxWidth >= breakpointLg
          ? 4
          : constraints.maxWidth >= breakpointSm
              ? 3
              : 2;

      return GridView.builder(
        padding: const EdgeInsets.all(20),
        gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(
          crossAxisCount: crossAxisCount,
          childAspectRatio: 0.75,
          crossAxisSpacing: 16,
          mainAxisSpacing: 16,
        ),
        itemCount: products.length,
        itemBuilder: (context, index) {
          final product = products[index];
          return _ProductCard(
            product: product,
            onTap: () => _emitOpenProduct(product),
          );
        },
      );
    });
  }

  void _emitOpenProduct(Map<String, dynamic> product) {
    setFormField('p.productId', product['id']);
    submit(_onOpenProduct);
  }
}

// :: ProductCard

class _ProductCard extends StatelessWidget {
  final Map<String, dynamic> product;
  final VoidCallback onTap;

  const _ProductCard({required this.product, required this.onTap});

  @override
  Widget build(BuildContext context) {
    final name = product['name'] as String? ?? '';
    final price = (product['price'] as num?)?.toDouble() ?? 0.0;
    final id = product['id'];

    return HoverCard(
      onTap: onTap,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Expanded(
            child: Container(
              decoration: const BoxDecoration(gradient: imageGradient),
              padding: const EdgeInsets.all(20),
              child: id != null
                  ? Image.network(
                      resolveAssetUrl('./image/product/$id.png'),
                      fit: BoxFit.contain,
                      errorBuilder: (_, __, ___) => imageErrorPlaceholder(),
                    )
                  : imageErrorPlaceholder(),
            ),
          ),
          Container(
            color: appSurface,
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(name,
                    style: const TextStyle(fontSize: 13, fontWeight: FontWeight.w500),
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis),
                const SizedBox(height: 6),
                Text(formatCurrency(price),
                    style: const TextStyle(fontSize: 15, fontWeight: FontWeight.w700, color: appAccent)),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
