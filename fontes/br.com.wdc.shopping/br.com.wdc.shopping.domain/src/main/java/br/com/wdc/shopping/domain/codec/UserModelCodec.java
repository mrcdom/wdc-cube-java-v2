package br.com.wdc.shopping.domain.codec;

import br.com.wdc.framework.commons.serialization.ExtensibleObjectInput;
import br.com.wdc.framework.commons.serialization.ExtensibleObjectOutput;
import br.com.wdc.framework.commons.serialization.InputCoerceUtils;
import br.com.wdc.shopping.domain.criteria.UserCriteria;
import br.com.wdc.shopping.domain.model.User;

public class UserModelCodec implements ModelCodec<User, UserCriteria> {

	@Override
	public void writeEntity(ExtensibleObjectOutput out, User entity) {
		out.beginObject();
		if (entity.id != null) out.name("id").value(entity.id);
		if (entity.userName != null) out.name("userName").value(entity.userName);
		if (entity.name != null) out.name("name").value(entity.name);
		if (entity.password != null) out.name("password").value(entity.password);
		if (entity.roles != null) out.name("roles").value(entity.roles);
		out.endObject();
	}

	@Override
	public User readEntity(ExtensibleObjectInput in) {
		var user = new User();
		in.beginObject();
		while (in.hasNext()) {
			switch (in.nextName()) {
				case "id" -> user.id = InputCoerceUtils.asLong(in);
				case "userName" -> user.userName = InputCoerceUtils.asString(in);
				case "name" -> user.name = InputCoerceUtils.asString(in);
				case "password" -> user.password = InputCoerceUtils.asString(in);
				case "roles" -> user.roles = InputCoerceUtils.asString(in);
				default -> in.skipValue();
			}
		}
		in.endObject();
		return user;
	}

	@Override
	public void writeCriteriaFields(ExtensibleObjectOutput out, UserCriteria criteria) {
		if (criteria.userId() != null) out.name("userId").value(criteria.userId());
		if (criteria.userName() != null) out.name("userName").value(criteria.userName());
		if (criteria.password() != null) out.name("password").value(criteria.password());
		if (criteria.orderBy() != null) out.name("orderBy").value(criteria.orderBy().name());
	}

	@Override
	public User getProjection(UserCriteria criteria) {
		return criteria.projection();
	}

	@Override
	public void setGeneratedId(User entity, long id) {
		entity.id = id;
	}
}
