import 'package:flutter/material.dart';

import '../bridge/view_state_coordinator.dart';
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
      final crossAxisCount = constraints.maxWidth >= 900
          ? 4
          : constraints.maxWidth >= 600
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

class _ProductCard extends StatefulWidget {
  final Map<String, dynamic> product;
  final VoidCallback onTap;

  const _ProductCard({required this.product, required this.onTap});

  @override
  State<_ProductCard> createState() => _ProductCardState();
}

class _ProductCardState extends State<_ProductCard> {
  bool _hovered = false;

  @override
  Widget build(BuildContext context) {
    final name = widget.product['name'] as String? ?? '';
    final price = (widget.product['price'] as num?)?.toDouble() ?? 0.0;
    final id = widget.product['id'];
    final imageUrl = './image/product/$id.png';

    return MouseRegion(
      cursor: SystemMouseCursors.click,
      onEnter: (_) => setState(() => _hovered = true),
      onExit: (_) => setState(() => _hovered = false),
      child: GestureDetector(
        onTap: widget.onTap,
        child: AnimatedContainer(
          duration: const Duration(milliseconds: 200),
          curve: Curves.easeOut,
          transform: _hovered ? (Matrix4.identity()..translate(0.0, -3.0)) : Matrix4.identity(),
          decoration: BoxDecoration(
            color: Colors.white,
            borderRadius: BorderRadius.circular(12),
            border: Border.all(color: const Color(0xFFE5E5EA)),
            boxShadow: _hovered
                ? const [
                    BoxShadow(color: Color(0x1A000000), blurRadius: 12, offset: Offset(0, 8)),
                    BoxShadow(color: Color(0x0D000000), blurRadius: 4, offset: Offset(0, 2)),
                  ]
                : const [
                    BoxShadow(color: Color(0x0F000000), blurRadius: 3, offset: Offset(0, 1)),
                    BoxShadow(color: Color(0x0A000000), blurRadius: 2, offset: Offset(0, 1)),
                  ],
          ),
          clipBehavior: Clip.antiAlias,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Expanded(
                child: Container(
                  decoration: const BoxDecoration(
                    gradient: LinearGradient(
                      begin: Alignment.topCenter,
                      end: Alignment.bottomCenter,
                      colors: [Color(0xFFF8FAFC), Color(0xFFEEF2F7)],
                    ),
                  ),
                  padding: const EdgeInsets.all(20),
                  child: Image.network(
                    _resolveImageUrl(imageUrl),
                    fit: BoxFit.contain,
                    errorBuilder: (_, __, ___) => const Icon(Icons.image_not_supported, size: 48, color: Colors.grey),
                  ),
                ),
              ),
              Container(
                color: Colors.white,
                padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(name,
                        style: const TextStyle(fontSize: 13, fontWeight: FontWeight.w500),
                        maxLines: 2,
                        overflow: TextOverflow.ellipsis),
                    const SizedBox(height: 6),
                    Text('R\$ ${price.toStringAsFixed(2)}',
                        style: const TextStyle(fontSize: 15, fontWeight: FontWeight.w700, color: Color(0xFF0D66D0))),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  static String _resolveImageUrl(String relativePath) {
    // Resolve relative to backend base URL
    final base = ViewStateCoordinator.instance.baseWebSocketUrl.replaceFirst('ws', 'http');
    final baseUri = Uri.parse(base);
    return '${baseUri.scheme}://${baseUri.host}:${baseUri.port}/$relativePath';
  }
}
