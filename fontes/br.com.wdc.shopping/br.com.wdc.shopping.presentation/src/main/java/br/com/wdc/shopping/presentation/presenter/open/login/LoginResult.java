package br.com.wdc.shopping.presentation.presenter.open.login;

import br.com.wdc.shopping.presentation.presenter.open.login.structs.Subject;

/**
 * Resultado de uma tentativa de autenticação bem-sucedida.
 *
 * @param subject         dados do usuário autenticado
 * @param persistentToken token de auto-login (remember me); {@code null} em clientes REST
 */
public record LoginResult(
        Subject subject,
        String persistentToken) {
}
