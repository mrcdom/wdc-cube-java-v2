import 'package:flutter/material.dart';

import '../bridge/view_state_coordinator.dart';
import 'base_view.dart';

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
      final wide = constraints.maxWidth >= 768;
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
        gradient: LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [Color(0xFF0D66D0), Color(0xFF1A8CFF), Color(0xFF4DA6FF)],
          stops: [0.0, 0.4, 1.0],
        ),
      ),
      child: Stack(
        children: [
          // Decorative circles
          Positioned(
            top: -60,
            right: -60,
            child: Container(
              width: 200,
              height: 200,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                color: Colors.white.withValues(alpha: 0.06),
              ),
            ),
          ),
          Positioned(
            bottom: -40,
            left: -40,
            child: Container(
              width: 160,
              height: 160,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                color: Colors.white.withValues(alpha: 0.04),
              ),
            ),
          ),
          Positioned(
            top: 140,
            left: 60,
            child: Container(
              width: 80,
              height: 80,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                color: Colors.white.withValues(alpha: 0.05),
              ),
            ),
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

  Widget _buildForm(BuildContext context, String? errorMessage, bool loading) {
    return Center(
      child: SingleChildScrollView(
        padding: const EdgeInsets.all(32),
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 380),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              ClipRRect(
                  borderRadius: BorderRadius.circular(16),
                  child: Container(
                    padding: const EdgeInsets.all(24),
                    decoration: const BoxDecoration(
                      gradient: LinearGradient(
                        begin: Alignment.topLeft,
                        end: Alignment.bottomRight,
                        colors: [Color(0xFF0D66D0), Color(0xFF1A8CFF), Color(0xFF4DA6FF)],
                        stops: [0.0, 0.4, 1.0],
                      ),
                    ),
                    child: Stack(
                      alignment: Alignment.center,
                      children: [
                        Positioned(
                          top: -30,
                          right: -30,
                          child: Container(
                            width: 100,
                            height: 100,
                            decoration: BoxDecoration(
                              shape: BoxShape.circle,
                              color: Colors.white.withValues(alpha: 0.06),
                            ),
                          ),
                        ),
                        Positioned(
                          bottom: -20,
                          left: -20,
                          child: Container(
                            width: 80,
                            height: 80,
                            decoration: BoxDecoration(
                              shape: BoxShape.circle,
                              color: Colors.white.withValues(alpha: 0.04),
                            ),
                          ),
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
                const SizedBox(height: 24),
              const Text('Bem-vindo', style: TextStyle(fontSize: 24, fontWeight: FontWeight.bold)),
              const SizedBox(height: 4),
              Text('Entre com suas credenciais para continuar',
                  style: TextStyle(fontSize: 14, color: Colors.grey.shade600)),
              const SizedBox(height: 24),
              if (errorMessage != null) ...[
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
                  child: loading
                      ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                      : const Text('Entrar', style: TextStyle(fontSize: 16)),
                ),
              ),
              const SizedBox(height: 16),
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: Colors.grey.shade50,
                  borderRadius: BorderRadius.circular(8),
                  border: Border.all(color: Colors.grey.shade200),
                ),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    Text('Acesso demo: ', style: TextStyle(fontSize: 12, color: Colors.grey.shade600)),
                    const Text('admin', style: TextStyle(fontSize: 12, fontWeight: FontWeight.bold, color: Color(0xFF0D66D0))),
                    Text(' / ', style: TextStyle(fontSize: 12, color: Colors.grey.shade600)),
                    const Text('admin', style: TextStyle(fontSize: 12, fontWeight: FontWeight.bold, color: Color(0xFF0D66D0))),
                  ],
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
    final encrypted = ViewStateCoordinator.instance.cipher(password);
    setFormField('p.password', encrypted);
    submit(_onLogin);
  }
}
