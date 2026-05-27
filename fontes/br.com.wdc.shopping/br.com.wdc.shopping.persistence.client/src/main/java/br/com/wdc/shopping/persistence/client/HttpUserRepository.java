package br.com.wdc.shopping.persistence.client;

import br.com.wdc.shopping.domain.codec.ModelCodec;
import br.com.wdc.shopping.domain.criteria.UserCriteria;
import br.com.wdc.shopping.domain.model.User;
import br.com.wdc.shopping.domain.repositories.UserRepository;

public class HttpUserRepository extends HttpRepository<User, UserCriteria, Long> implements UserRepository {

    public HttpUserRepository(HttpTransport transport, ModelCodec<User, UserCriteria> codec) {
        super(transport, codec, "/api/repo/user");
    }
}
