import 'package:flutter/widgets.dart';

import '../bridge/view_state_coordinator.dart';
import 'root_view.dart';
import 'slot_view.dart';
import 'login_view.dart';
import 'home_view.dart';
import 'products_panel.dart';
import 'purchases_panel.dart';
import 'cart_view.dart';
import 'product_view.dart';
import 'receipt_view.dart';

/// Registers all shopping view factories with the coordinator.
void registerAllViews(ViewStateCoordinator coordinator) {
  coordinator.registerView(RootView.viewId, (vsid, _) => RootView(key: ValueKey(vsid), vsid: vsid));
  coordinator.registerView(SlotView.viewId, (vsid, _) => SlotView(key: ValueKey(vsid), vsid: vsid));
  coordinator.registerView(LoginView.viewId, (vsid, _) => LoginView(key: ValueKey(vsid), vsid: vsid));
  coordinator.registerView(HomeView.viewId, (vsid, _) => HomeView(key: ValueKey(vsid), vsid: vsid));
  coordinator.registerView(ProductsPanel.viewId, (vsid, _) => ProductsPanel(key: ValueKey(vsid), vsid: vsid));
  coordinator.registerView(PurchasesPanel.viewId, (vsid, _) => PurchasesPanel(key: ValueKey(vsid), vsid: vsid));
  coordinator.registerView(CartView.viewId, (vsid, _) => CartView(key: ValueKey(vsid), vsid: vsid));
  coordinator.registerView(ProductView.viewId, (vsid, _) => ProductView(key: ValueKey(vsid), vsid: vsid));
  coordinator.registerView(ReceiptView.viewId, (vsid, _) => ReceiptView(key: ValueKey(vsid), vsid: vsid));
}
