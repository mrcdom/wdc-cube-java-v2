package br.com.wdc.shopping.domain.codec;

import br.com.wdc.framework.domain.codec.ModelCodec;

import br.com.wdc.framework.commons.serialization.EntityGraph;
import br.com.wdc.framework.commons.serialization.ExtensibleObjectInput;
import br.com.wdc.framework.commons.serialization.ExtensibleObjectOutput;
import br.com.wdc.framework.commons.serialization.InputCoerceUtils;
import br.com.wdc.shopping.domain.criteria.UserCriteria;
import br.com.wdc.shopping.domain.model.User;
import br.com.wdc.framework.domain.projection.ProjectionValues;

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
	public void writeEntity(ExtensibleObjectOutput out, User entity, EntityGraph graph) {
		if (!graph.track(entity)) {
			out.beginObject();
			if (entity.id != null) out.name("id").value(entity.id);
			out.endObject();
			return;
		}
		writeEntity(out, entity);
	}

	@Override
	public void writeEntityProjected(ExtensibleObjectOutput out, User entity, User projection) {
		out.beginObject();
		if (entity.id != null) out.name("id").value(entity.id);
		if (projection.userName != null) {
			out.name("userName");
			if (entity.userName != null) out.value(entity.userName); else out.nullValue();
		}
		if (projection.name != null) {
			out.name("name");
			if (entity.name != null) out.value(entity.name); else out.nullValue();
		}
		if (projection.password != null) {
			out.name("password");
			if (entity.password != null) out.value(entity.password); else out.nullValue();
		}
		if (projection.roles != null) {
			out.name("roles");
			if (entity.roles != null) out.value(entity.roles); else out.nullValue();
		}
		out.endObject();
	}

	@Override
	public User computeProjection(User newEntity, User oldEntity) {
		var pv = ProjectionValues.INSTANCE;
		var projection = new User();
		if (!java.util.Objects.equals(newEntity.userName, oldEntity.userName)) projection.userName = pv.str;
		if (!java.util.Objects.equals(newEntity.name, oldEntity.name)) projection.name = pv.str;
		if (!java.util.Objects.equals(newEntity.password, oldEntity.password)) projection.password = pv.str;
		if (!java.util.Objects.equals(newEntity.roles, oldEntity.roles)) projection.roles = pv.str;
		return projection;
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
