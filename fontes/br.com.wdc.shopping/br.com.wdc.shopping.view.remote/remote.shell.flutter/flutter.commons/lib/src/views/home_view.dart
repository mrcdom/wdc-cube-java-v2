import 'package:flutter/material.dart';

import '../bridge/view_state_coordinator.dart';
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
          vsid: widget.vsid,
          nickName: nickName,
          cartItemCount: cartItemCount,
        ),
        if (errorMessage != null)
          Container(
            margin: const EdgeInsets.all(8),
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
        Expanded(
          child: contentViewId != null
              ? _buildChildContent(contentViewId)
              : _buildSplitContent(productsPanelViewId, purchasesPanelViewId),
        ),
      ],
    );
  }

  Widget _buildChildContent(String contentViewId) {
    final view = ViewStateCoordinator.instance.createView(contentViewId);
    if (view is Widget) return view;
    return const SizedBox.shrink();
  }

  Widget _buildSplitContent(String? productsPanelViewId, String? purchasesPanelViewId) {
    return LayoutBuilder(builder: (context, constraints) {
      final wide = constraints.maxWidth >= 768;

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
            child: _showingProducts ? _buildPanel(productsPanelViewId) : _buildPanel(purchasesPanelViewId),
          ),
        ],
      );
    });
  }

  Widget _buildPanel(String? panelViewId) {
    if (panelViewId == null) return const SizedBox.shrink();
    final view = ViewStateCoordinator.instance.createView(panelViewId);
    if (view is Widget) return view;
    return const SizedBox.shrink();
  }

  void _switchTab(bool showProducts) {
    setState(() => _showingProducts = showProducts);
  }
}

// :: HeaderPanel

class _HeaderPanel extends StatelessWidget {
  final String vsid;
  final String nickName;
  final int cartItemCount;

  const _HeaderPanel({required this.vsid, required this.nickName, required this.cartItemCount});

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(builder: (context, constraints) {
      final isExpanded = constraints.maxWidth >= 576;
      return Container(
        decoration: const BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
            colors: [Color(0xFF0D66D0), Color(0xFF1A8CFF)],
          ),
          boxShadow: [
            BoxShadow(color: Color(0x4D0D66D0), blurRadius: 8, offset: Offset(0, 2)),
          ],
        ),
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
        child: Row(
          children: [
            // Left: exit + greeting
            IconButton(
              icon: const Icon(Icons.logout, color: Colors.white),
              onPressed: () => _emitExit(context),
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
            GestureDetector(
              onTap: () => _emitOpenCart(context),
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
                        style: const TextStyle(fontSize: 11, color: Color(0xFF0D66D0), fontWeight: FontWeight.bold)),
                  ),
                ],
              ),
            ),
          ],
        ),
      );
    });
  }

  void _emitExit(BuildContext context) {
    final coord = ViewStateCoordinator.instance;
    coord.submit(vsid, _onExit);
  }

  void _emitOpenCart(BuildContext context) {
    final coord = ViewStateCoordinator.instance;
    coord.submit(vsid, _onOpenCart);
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
      decoration: BoxDecoration(
        border: Border(bottom: BorderSide(color: Colors.grey.shade200)),
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
    final color = active ? Theme.of(context).colorScheme.primary : Colors.grey;
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
