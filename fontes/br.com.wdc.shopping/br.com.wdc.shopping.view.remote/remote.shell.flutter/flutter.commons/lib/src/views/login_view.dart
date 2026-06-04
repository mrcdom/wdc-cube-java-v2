import 'package:flutter/material.dart';

import 'package:remote_bridge_flutter/remote_bridge_flutter.dart';
import '../design_tokens.dart';
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
    final state = viewState;
    final errorMessage = state['errorMessage'] as String?;
    final loading = state['loading'] == true;

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
                      color: Colors.white.withValues(alpha: 0.12),
                      borderRadius: BorderRadius.circular(20),
                      border: Border.all(color: Colors.white.withValues(alpha: 0.2)),
                    ),
                    child: const Icon(Icons.shopping_bag_outlined, size: 40, color: Colors.white),
                  ),
                  const SizedBox(height: 16),
                  const Text('WDC Shopping',
                      style: TextStyle(fontSize: 28, fontWeight: FontWeight.bold, color: Colors.white)),
                  const SizedBox(height: 8),
                  Text('Sua compra certa na internet.',
                      style: TextStyle(fontSize: 16, color: Colors.white.withValues(alpha: 0.8))),
                  const SizedBox(height: 32),
                  _featureRow(Icons.shield_outlined, 'Compra segura'),
                  const SizedBox(height: 12),
                  _featureRow(Icons.local_shipping_outlined, 'Entrega rápida'),
                  const SizedBox(height: 12),
                  _featureRow(Icons.autorenew, 'Troca garantida'),
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
        Icon(icon, color: Colors.white.withValues(alpha: 0.9), size: 20),
        const SizedBox(width: 8),
        Text(text, style: TextStyle(color: Colors.white.withValues(alpha: 0.9), fontSize: 14)),
      ],
    );
  }

  Widget _decorativeCircle(double size, double alpha) => Container(
    width: size,
    height: size,
    decoration: BoxDecoration(
      shape: BoxShape.circle,
      color: Colors.white.withValues(alpha: alpha),
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
                              color: Colors.white.withValues(alpha: 0.12),
                              borderRadius: BorderRadius.circular(12),
                              border: Border.all(color: Colors.white.withValues(alpha: 0.2)),
                            ),
                            child: const Icon(Icons.shopping_bag_outlined, size: 24, color: Colors.white),
                          ),
                          const SizedBox(height: 8),
                          const Text('WDC Shopping',
                              style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: Colors.white)),
                          const Text('Sua compra certa na internet.',
                              style: TextStyle(fontSize: 12, color: Colors.white70)),
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
                      const Text('Bem-vindo', style: TextStyle(fontSize: 24, fontWeight: FontWeight.bold)),
                      const SizedBox(height: 4),
                      const Text('Entre com suas credenciais para continuar',
                          style: TextStyle(fontSize: 14, color: appTextSecondary)),
                      const SizedBox(height: 28),
                      if (errorMessage != null) ...[
                        ErrorBanner(message: errorMessage),
                        const SizedBox(height: 16),
                      ],
                      const Text('Usuário', style: TextStyle(fontSize: 13, fontWeight: FontWeight.w500)),
                      const SizedBox(height: 4),
                      TextField(
                        controller: _userController,
                        enabled: !loading,
                        decoration: const InputDecoration(
                          hintText: 'Digite seu usuário',
                          border: OutlineInputBorder(),
                          isDense: true,
                        ),
                        onSubmitted: (_) => _emitLogin(),
                      ),
                      const SizedBox(height: 16),
                      const Text('Senha', style: TextStyle(fontSize: 13, fontWeight: FontWeight.w500)),
                      const SizedBox(height: 4),
                      TextField(
                        controller: _passController,
                        enabled: !loading,
                        obscureText: true,
                        decoration: const InputDecoration(
                          hintText: 'Digite sua senha',
                          border: OutlineInputBorder(),
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
                              ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                              : const Text('Entrar', style: TextStyle(fontSize: 16)),
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
                            const Text('Acesso demo: ', style: TextStyle(fontSize: 12, color: appTextSecondary)),
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
    final security = ViewStateCoordinator.instance.dataSecurity;
    final encrypted = security.isReady ? security.b64Cipher(password) : password;
    setFormField('p.password', encrypted);
    submit(_onLogin);
  }
}
