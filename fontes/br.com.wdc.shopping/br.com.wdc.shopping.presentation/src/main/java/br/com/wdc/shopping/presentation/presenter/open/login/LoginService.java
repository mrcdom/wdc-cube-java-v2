package br.com.wdc.shopping.presentation.presenter.open.login;

import br.com.wdc.shopping.business.shared.criteria.UserCriteria;
import br.com.wdc.shopping.business.shared.repositories.UserRepository;
import br.com.wdc.shopping.presentation.presenter.open.login.structs.Subject;

public enum LoginService {
    BEAN;

    public Subject fetchSubject(String userName, String password) {
        var repository = UserRepository.BEAN.get();
        return repository.fetch(new UserCriteria()
                .withUserName(userName)
                .withPassword(password)
                .withProjection(Subject.projection())
                .withLimit(1))
                .stream().map(Subject::create)
                .findFirst().orElse(null);
    }

}