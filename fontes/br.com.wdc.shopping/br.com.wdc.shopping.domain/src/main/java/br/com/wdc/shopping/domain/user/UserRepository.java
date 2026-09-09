package br.com.wdc.shopping.domain.user;

import java.util.concurrent.atomic.AtomicReference;

import br.com.wdc.framework.domain.projection.ProjectionValues;
import br.com.wdc.framework.domain.repository.Repository;

public interface UserRepository extends Repository<User, UserCriteria, Long> {

    AtomicReference<UserRepository> BEAN = new AtomicReference<>();
    
    @Override
    default User newProjection() {
        var pv = ProjectionValues.INSTANCE;

        return new User()
                .withId(pv.i64)
                .withUserName(pv.str)
                .withPassword(pv.str)
                .withName(pv.str)
                .withRoles(pv.str);
    }

    /**
     * Busca pela chave — um {@code userId} com filtro de igualdade sobre a chave primária.
     *
     * <p>
     * Mora aqui, e não em {@code Repository}, porque o contrato genérico não tem como montar o critério: ele conhece
     * o tipo {@code C}, mas não qual dos campos dele é a chave. Cada entidade sabe, e é só o que falta — o resto é o
     * {@code fetch} que todas as implementações já têm. Assim a busca por chave é a mesma consulta das outras, com o
     * mesmo tratamento de projeção, de segurança e de transação, em vez de um caminho paralelo por implementação.
     * </p>
     *
     * @param projection {@code null} projeta {@link #newProjection()} — todos os campos rasos da entidade.
     * @return a entidade, ou {@code null} se não houver linha com essa chave.
     */
    @Override
    default User fetchById(Long userId, User projection) {
        if (userId == null) {
            return null;
        }

        var found = fetch(new UserCriteria()
                .withUserId(userId)
                .withProjection(projection != null ? projection : newProjection()), 0, 1);

        return found.isEmpty() ? null : found.get(0);
    }

}
