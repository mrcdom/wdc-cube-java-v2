import 'package:flutter/material.dart';

import '../design_tokens.dart';
import '../widgets/error_banner.dart';
import 'base_view.dart';

/// Actions
const _onExit = 1;
const _onOpenCart = 2;

/// HomeView — main application view with header, tabs (products/purchases), and child content.
class HomeView extends BaseView {
  const HomeView({super.key, required super.vsid});

  static const viewId = '473dbdd7a36a';

  @override
  State<HomeView> createState() => _HomeViewState();
}

class _HomeViewState extends BaseViewState<HomeView> {
  bool _showingProducts = true;

  @override
  Widget build(BuildContext context) {
    final state = viewState;
    final nickName = state['nickName'] as String? ?? '';
    final cartItemCount = (state['cartItemCount'] as num?)?.toInt() ?? 0;
    final errorMessage = state['errorMessage'] as String?;
    final contentViewId = state['contentViewId'] as String?;
    final productsPanelViewId = state['productsPanelViewId'] as String?;
    final purchasesPanelViewId = state['purchasesPanelViewId'] as String?;

    return Column(
      children: [
        _HeaderPanel(
          nickName: nickName,
          cartItemCount: cartItemCount,
          onExit: () => submit(_onExit),
          onOpenCart: () => submit(_onOpenCart),
        ),
        if (errorMessage != null)
          Padding(
            padding: const EdgeInsets.all(8),
            child: ErrorBanner(message: errorMessage),
          ),
        Expanded(
          child: contentViewId != null
              ? _buildChildContent(contentViewId)
              : _buildSplitContent(productsPanelViewId, purchasesPanelViewId),
        ),
      ],
    );
  }

  Widget _buildChildContent(String contentViewId) {
    return slot(contentViewId);
  }

  Widget _buildSplitContent(String? productsPanelViewId, String? purchasesPanelViewId) {
    return LayoutBuilder(builder: (context, constraints) {
      final wide = constraints.maxWidth >= breakpointMd;

      if (wide) {
        return Row(
          children: [
            Expanded(child: _buildPanel(productsPanelViewId)),
            SizedBox(width: 340, child: _buildPanel(purchasesPanelViewId)),
          ],
        );
      }

      // Mobile: tab navigation
      return Column(
        children: [
          _TabNav(showingProducts: _showingProducts, onSwitch: _switchTab),
          Expanded(
            child: IndexedStack(
              index: _showingProducts ? 0 : 1,
              children: [
                _buildPanel(productsPanelViewId),
                _buildPanel(purchasesPanelViewId),
              ],
            ),
          ),
        ],
      );
    });
  }

  Widget _buildPanel(String? panelViewId) {
    if (panelViewId == null) return const SizedBox.shrink();
    return slot(panelViewId);
  }

  void _switchTab(bool showProducts) {
    setState(() => _showingProducts = showProducts);
  }
}

// :: HeaderPanel

class _HeaderPanel extends StatelessWidget {
  final String nickName;
  final int cartItemCount;
  final VoidCallback onExit;
  final VoidCallback onOpenCart;

  const _HeaderPanel({required this.nickName, required this.cartItemCount, required this.onExit, required this.onOpenCart});

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(builder: (context, constraints) {
      final isExpanded = constraints.maxWidth >= breakpointSm;
      return Container(
        decoration: const BoxDecoration(
          gradient: headerGradient,
          boxShadow: headerShadow,
        ),
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
        child: Row(
          children: [
            // Left: exit + greeting
            IconButton(
              icon: const Icon(Icons.logout, color: Colors.white),
              onPressed: onExit,
            ),
            const SizedBox(width: 4),
            if (isExpanded)
              Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                mainAxisSize: MainAxisSize.min,
                children: [
                  Text('Bem-vindo(a),',
                      style: TextStyle(fontSize: 11, color: Colors.white.withValues(alpha: 0.7))),
                  Text(nickName,
                      style: const TextStyle(fontSize: 13, fontWeight: FontWeight.w600, color: Colors.white)),
                ],
              ),
            const Spacer(),
            // Center: logo
            const Icon(Icons.shopping_bag_outlined, color: Colors.white, size: 24),
            const SizedBox(width: 6),
            const Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisSize: MainAxisSize.min,
              children: [
                Text('Shopping',
                    style: TextStyle(fontSize: 15, fontWeight: FontWeight.bold, color: Colors.white)),
                Text('By WeDoCode', style: TextStyle(fontSize: 10, color: Colors.white60)),
              ],
            ),
            const Spacer(),
            // Right: cart button
            InkWell(
              onTap: onOpenCart,
              borderRadius: BorderRadius.circular(8),
              child: Padding(
                padding: const EdgeInsets.symmetric(horizontal: 4, vertical: 4),
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    const Icon(Icons.shopping_bag_outlined, color: Colors.white, size: 24),
                    if (isExpanded) ...[
                      const SizedBox(width: 6),
                      const Text('Carrinho',
                          style: TextStyle(fontSize: 14, fontWeight: FontWeight.w500, color: Colors.white)),
                    ],
                    const SizedBox(width: 8),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                      decoration: BoxDecoration(
                        color: Colors.white,
                        borderRadius: BorderRadius.circular(10),
                        boxShadow: const [BoxShadow(color: Color(0x26000000), blurRadius: 4, offset: Offset(0, 2))],
                      ),
                      child: Text('$cartItemCount',
                          style: const TextStyle(fontSize: 11, color: appAccent, fontWeight: FontWeight.bold)),
                    ),
                  ],
                ),
              ),
            ),
          ],
        ),
      );
    });
  }
}

// :: TabNav

class _TabNav extends StatelessWidget {
  final bool showingProducts;
  final void Function(bool) onSwitch;

  const _TabNav({required this.showingProducts, required this.onSwitch});

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: const BoxDecoration(
        border: Border(bottom: BorderSide(color: appBorder)),
      ),
      child: Row(
        children: [
          Expanded(
            child: _TabButton(
              icon: Icons.grid_view,
              label: 'Produtos',
              active: showingProducts,
              onTap: () => onSwitch(true),
            ),
          ),
          Expanded(
            child: _TabButton(
              icon: Icons.history,
              label: 'Histórico',
              active: !showingProducts,
              onTap: () => onSwitch(false),
            ),
          ),
        ],
      ),
    );
  }
}

class _TabButton extends StatelessWidget {
  final IconData icon;
  final String label;
  final bool active;
  final VoidCallback onTap;

  const _TabButton({required this.icon, required this.label, required this.active, required this.onTap});

  @override
  Widget build(BuildContext context) {
    final color = active ? appAccent : appTextSecondary;
    return InkWell(
      onTap: onTap,
      child: Padding(
        padding: const EdgeInsets.symmetric(vertical: 10),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(icon, size: 18, color: color),
            const SizedBox(height: 2),
            Text(label, style: TextStyle(fontSize: 12, color: color, fontWeight: active ? FontWeight.w600 : FontWeight.normal)),
            const SizedBox(height: 4),
            Container(height: 2, color: active ? color : Colors.transparent),
          ],
        ),
      ),
    );
  }
}
