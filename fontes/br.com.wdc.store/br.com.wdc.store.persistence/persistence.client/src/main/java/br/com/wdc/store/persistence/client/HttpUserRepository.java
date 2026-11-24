package br.com.wdc.store.persistence.client;

import br.com.wdc.framework.commons.http.HttpTransport;
import br.com.wdc.framework.domain.codec.ModelCodec;
import br.com.wdc.store.domain.user.User;
import br.com.wdc.store.domain.user.UserCriteria;
import br.com.wdc.store.domain.user.UserRepository;

public class HttpUserRepository extends HttpRepository<User, UserCriteria, Long> implements UserRepository {

    public HttpUserRepository(HttpTransport transport, ModelCodec<User, UserCriteria> codec) {
        super(transport, codec, "/api/repo/user");
    }
}
