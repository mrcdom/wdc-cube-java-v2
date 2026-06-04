import 'package:flutter/material.dart';

import 'base_view.dart';

/// SlotView — just renders whatever child view the server assigns to its 'slot' field.
class SlotView extends BaseView {
  const SlotView({super.key, required super.vsid});

  static const viewId = '798574115fcd';

  @override
  State<SlotView> createState() => _SlotViewState();
}

class _SlotViewState extends BaseViewState<SlotView> {
  @override
  Widget build(BuildContext context) {
    final slotViewId = viewState['slot'] as String?;
    if (slotViewId == null) return const SizedBox.shrink();
    return slot(slotViewId);
  }
}
