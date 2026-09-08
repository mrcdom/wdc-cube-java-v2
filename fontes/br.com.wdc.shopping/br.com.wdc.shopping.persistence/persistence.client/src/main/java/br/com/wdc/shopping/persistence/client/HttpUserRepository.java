package br.com.wdc.shopping.persistence.client;

import br.com.wdc.framework.commons.http.HttpTransport;
import br.com.wdc.framework.domain.codec.ModelCodec;
import br.com.wdc.shopping.domain.user.User;
import br.com.wdc.shopping.domain.user.UserCriteria;
import br.com.wdc.shopping.domain.user.UserRepository;

public class HttpUserRepository extends HttpRepository<User, UserCriteria, Long> implements UserRepository {

    public HttpUserRepository(HttpTransport transport, ModelCodec<User, UserCriteria> codec) {
        super(transport, codec, "/api/repo/user");
    }
}
