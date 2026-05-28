package br.com.wdc.shopping.domain.codec;

import static br.com.wdc.shopping.domain.repositories.Repository.changed;

import br.com.wdc.framework.commons.serialization.ExtensibleObjectInput;
import br.com.wdc.framework.commons.serialization.ExtensibleObjectOutput;
import br.com.wdc.framework.commons.serialization.InputCoerceUtils;
import br.com.wdc.shopping.domain.criteria.UserCriteria;
import br.com.wdc.shopping.domain.model.User;
import br.com.wdc.shopping.domain.utils.ProjectionValues;

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
	public void writeEntityProjected(ExtensibleObjectOutput out, User newEntity, User oldEntity, User projection) {
		out.beginObject();
		if (newEntity.id != null) out.name("id").value(newEntity.id);
		if (changed(newEntity, oldEntity, projection, u -> u.userName)) {
			out.name("userName");
			if (newEntity.userName != null) out.value(newEntity.userName); else out.nullValue();
		}
		if (changed(newEntity, oldEntity, projection, u -> u.name)) {
			out.name("name");
			if (newEntity.name != null) out.value(newEntity.name); else out.nullValue();
		}
		if (changed(newEntity, oldEntity, projection, u -> u.password)) {
			out.name("password");
			if (newEntity.password != null) out.value(newEntity.password); else out.nullValue();
		}
		if (changed(newEntity, oldEntity, projection, u -> u.roles)) {
			out.name("roles");
			if (newEntity.roles != null) out.value(newEntity.roles); else out.nullValue();
		}
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
	public UpdateData<User> readEntityForUpdate(ExtensibleObjectInput in) {
		var pv = ProjectionValues.INSTANCE;
		var entity = new User();
		var projection = new User();
		in.beginObject();
		while (in.hasNext()) {
			switch (in.nextName()) {
				case "id" -> { entity.id = InputCoerceUtils.asLong(in); projection.id = pv.i64; }
				case "userName" -> { entity.userName = InputCoerceUtils.asString(in); projection.userName = pv.str; }
				case "name" -> { entity.name = InputCoerceUtils.asString(in); projection.name = pv.str; }
				case "password" -> { entity.password = InputCoerceUtils.asString(in); projection.password = pv.str; }
				case "roles" -> { entity.roles = InputCoerceUtils.asString(in); projection.roles = pv.str; }
				default -> in.skipValue();
			}
		}
		in.endObject();
		return new UpdateData<>(entity, projection);
	}

	@Override
	public void writeCriteriaFields(ExtensibleObjectOutput out, UserCriteria criteria) {
		if (criteria.userId() != null) out.name("userId").value(criteria.userId());
		if (criteria.userName() != null) out.name("userName").value(criteria.userName());
		if (criteria.password() != null) out.name("password").value(criteria.password());
		if (criteria.orderBy() != null) out.name("orderBy").value(criteria.orderBy().name());
	}

	@Override
	public boolean readCriteriaField(ExtensibleObjectInput in, String fieldName, UserCriteria criteria) {
		switch (fieldName) {
			case "userId" -> criteria.withUserId(InputCoerceUtils.asLong(in));
			case "userName" -> criteria.withUserName(InputCoerceUtils.asString(in));
			case "password" -> criteria.withPassword(InputCoerceUtils.asString(in));
			case "orderBy" -> {
				var v = InputCoerceUtils.asString(in);
				if (v != null) criteria.withOrderBy(UserCriteria.OrderBy.valueOf(v));
			}
			default -> { return false; }
		}
		return true;
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
