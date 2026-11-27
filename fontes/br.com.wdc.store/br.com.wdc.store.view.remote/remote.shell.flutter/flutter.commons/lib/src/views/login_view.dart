import 'package:flutter/material.dart';

import 'package:remote_bridge_flutter/remote_bridge_flutter.dart';
import '../design_tokens.dart';
import '../generated/messages.dart' show MessageKeys;
import '../i18n.dart';
import '../widgets/error_banner.dart';


/// Actions
const _onLogin = 1;

/// LoginView — login form with user/password fields.
class LoginView extends BaseView {
  const LoginView({super.key, required super.vsid});

  static const viewId = 'c677cda52d14';

  @override
  State<LoginView> createState() => _LoginViewState();
}

class _LoginViewState extends BaseViewState<LoginView> {
  final _userController = TextEditingController();
  final _passController = TextEditingController();

  @override
  void dispose() {
    _userController.dispose();
    _passController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final message = StoreI18n.message(scope['errorMessage']);
    final errorMessage = message.isEmpty ? null : message;
    final loading = scope['loading'] == true;

    return LayoutBuilder(builder: (context, constraints) {
      final wide = constraints.maxWidth >= breakpointMd;
      if (wide) {
        return Row(
          children: [
            Expanded(child: _buildLeftPanel(context)),
            Expanded(child: _buildForm(context, errorMessage, loading)),
          ],
        );
      }
      return _buildForm(context, errorMessage, loading);
    });
  }

  Widget _buildLeftPanel(BuildContext context) {
    return Container(
      decoration: const BoxDecoration(
        gradient: loginGradient,
      ),
      child: Stack(
        children: [
          // Decorative circles
          Positioned(
            top: -60, right: -60,
            child: _decorativeCircle(200, 0.06),
          ),
          Positioned(
            bottom: -40, left: -40,
            child: _decorativeCircle(160, 0.04),
          ),
          Positioned(
            top: 140, left: 60,
            child: _decorativeCircle(80, 0.05),
          ),
          // Content
          Center(
            child: Padding(
              padding: const EdgeInsets.all(32),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Container(
                    width: 80,
                    height: 80,
                    decoration: BoxDecoration(
                      color: appOnPrimary.withValues(alpha: 0.12),
                      borderRadius: BorderRadius.circular(radiusRound),
                      border: Border.all(color: appOnPrimary.withValues(alpha: 0.2)),
                    ),
                    child: const Icon(Icons.shopping_bag_outlined, size: 40, color: appOnPrimary),
                  ),
                  const SizedBox(height: 16),
                  Text(StoreI18n.text(MessageKeys.appName),
                      style: const TextStyle(fontSize: 28, fontWeight: FontWeight.bold, color: appOnPrimary)),
                  const SizedBox(height: 8),
                  Text(StoreI18n.text(MessageKeys.appTagline),
                      style: TextStyle(fontSize: 16, color: appOnPrimary.withValues(alpha: 0.8))),
                  const SizedBox(height: 32),
                  _featureRow(Icons.shield_outlined, StoreI18n.text(MessageKeys.loginFeatureSecureCheckout)),
                  const SizedBox(height: 12),
                  _featureRow(Icons.local_shipping_outlined, StoreI18n.text(MessageKeys.loginFeatureFastDelivery)),
                  const SizedBox(height: 12),
                  _featureRow(Icons.autorenew, StoreI18n.text(MessageKeys.loginFeatureReturns)),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _featureRow(IconData icon, String text) {
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Icon(icon, color: appOnPrimary.withValues(alpha: 0.9), size: 20),
        const SizedBox(width: 8),
        Text(text, style: TextStyle(color: appOnPrimary.withValues(alpha: 0.9), fontSize: 14)),
      ],
    );
  }

  Widget _decorativeCircle(double size, double alpha) => Container(
    width: size,
    height: size,
    decoration: BoxDecoration(
      shape: BoxShape.circle,
      color: appOnPrimary.withValues(alpha: alpha),
    ),
  );

  Widget _buildForm(BuildContext context, String? errorMessage, bool loading) {
    return Center(
      child: SingleChildScrollView(
        padding: const EdgeInsets.all(24),
        child: Container(
          constraints: const BoxConstraints(maxWidth: 460),
          decoration: BoxDecoration(
            color: appSurface,
            borderRadius: BorderRadius.circular(radiusLg),
            boxShadow: cardShadowLg,
          ),
          clipBehavior: Clip.antiAlias,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              // Header gradient — flush top, inset sides, square corners
              Padding(
                padding: const EdgeInsets.fromLTRB(24, 0, 24, 0),
                child: Container(
                  padding: const EdgeInsets.symmetric(vertical: 28, horizontal: 16),
                  decoration: const BoxDecoration(gradient: loginGradient),
                  clipBehavior: Clip.antiAlias,
                  child: Stack(
                    alignment: Alignment.center,
                    clipBehavior: Clip.none,
                    children: [
                      Positioned(
                        top: -50, right: -40,
                        child: _decorativeCircle(120, 0.08),
                      ),
                      Positioned(
                        bottom: -40, left: -30,
                        child: _decorativeCircle(100, 0.06),
                      ),
                      Column(
                        mainAxisSize: MainAxisSize.min,
                        crossAxisAlignment: CrossAxisAlignment.center,
                        children: [
                          Container(
                            width: 48,
                            height: 48,
                            decoration: BoxDecoration(
                              color: appOnPrimary.withValues(alpha: 0.12),
                              borderRadius: BorderRadius.circular(radiusLg),
                              border: Border.all(color: appOnPrimary.withValues(alpha: 0.2)),
                            ),
                            child: const Icon(Icons.shopping_bag_outlined, size: 24, color: appOnPrimary),
                          ),
                          const SizedBox(height: 8),
                          Text(StoreI18n.text(MessageKeys.appName),
                              style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: appOnPrimary)),
                          Text(StoreI18n.text(MessageKeys.appTagline),
                              style: TextStyle(fontSize: 12, color: appOnPrimary.withValues(alpha: 0.7))),
                        ],
                      ),
                    ],
                  ),
                ),
              ),
              // Form content — larger horizontal padding
              Padding(
                padding: const EdgeInsets.fromLTRB(48, 28, 48, 32),
                child: ConstrainedBox(
                  constraints: const BoxConstraints(maxWidth: 320),
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      Text(StoreI18n.text(MessageKeys.loginWelcome), style: const TextStyle(fontSize: 24, fontWeight: FontWeight.bold)),
                      const SizedBox(height: 4),
                      Text(StoreI18n.text(MessageKeys.loginSubtitle),
                          style: const TextStyle(fontSize: 14, color: appTextSecondary)),
                      const SizedBox(height: 28),
                      if (errorMessage != null) ...[
                        ErrorBanner(message: errorMessage),
                        const SizedBox(height: 16),
                      ],
                      Text(StoreI18n.text(MessageKeys.loginUsername), style: const TextStyle(fontSize: 13, fontWeight: FontWeight.w500)),
                      const SizedBox(height: 4),
                      TextField(
                        controller: _userController,
                        enabled: !loading,
                        decoration: InputDecoration(
                          hintText: StoreI18n.text(MessageKeys.loginUsernamePlaceholder),
                          border: const OutlineInputBorder(),
                          isDense: true,
                        ),
                        onSubmitted: (_) => _emitLogin(),
                      ),
                      const SizedBox(height: 16),
                      Text(StoreI18n.text(MessageKeys.loginPassword), style: const TextStyle(fontSize: 13, fontWeight: FontWeight.w500)),
                      const SizedBox(height: 4),
                      TextField(
                        controller: _passController,
                        enabled: !loading,
                        obscureText: true,
                        decoration: InputDecoration(
                          hintText: StoreI18n.text(MessageKeys.loginPasswordPlaceholder),
                          border: const OutlineInputBorder(),
                          isDense: true,
                        ),
                        onSubmitted: (_) => _emitLogin(),
                      ),
                      const SizedBox(height: 24),
                      SizedBox(
                        height: 48,
                        child: FilledButton(
                          onPressed: loading ? null : _emitLogin,
                          style: accentButtonStyle,
                          child: loading
                              ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2, color: appOnPrimary))
                              : Text(StoreI18n.text(MessageKeys.loginSignIn), style: const TextStyle(fontSize: 16)),
                        ),
                      ),
                      const SizedBox(height: 20),
                      Container(
                        padding: const EdgeInsets.all(12),
                        decoration: BoxDecoration(
                          color: appBg,
                          borderRadius: BorderRadius.circular(radiusSm),
                          border: Border.all(color: appBorder),
                        ),
                        child: Row(
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: [
                            Text('${StoreI18n.text(MessageKeys.loginDemoAccess)} ', style: const TextStyle(fontSize: 12, color: appTextSecondary)),
                            const Text('admin', style: TextStyle(fontSize: 12, fontWeight: FontWeight.bold, color: appAccent)),
                            const Text(' / ', style: TextStyle(fontSize: 12, color: appTextSecondary)),
                            const Text('admin', style: TextStyle(fontSize: 12, fontWeight: FontWeight.bold, color: appAccent)),
                          ],
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  void _emitLogin() {
    final userName = _userController.text;
    final password = _passController.text;
    setFormField('p.userName', userName);
    // Ciphered with the connection key when the request goes out.
    setSecretFormField('p.password', password);
    submit(_onLogin);
  }
}
