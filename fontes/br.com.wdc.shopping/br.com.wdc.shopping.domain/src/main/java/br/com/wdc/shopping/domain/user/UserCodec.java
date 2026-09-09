package br.com.wdc.shopping.domain.user;

import br.com.wdc.framework.commons.serialization.EntityGraph;
import br.com.wdc.framework.commons.serialization.ExtensibleObjectInput;
import br.com.wdc.framework.commons.serialization.ExtensibleObjectOutput;
import br.com.wdc.framework.commons.serialization.InputCoerceUtils;
import br.com.wdc.framework.domain.codec.ModelCodec;
import br.com.wdc.framework.domain.criteria.CriterionCodec;
import br.com.wdc.framework.domain.projection.ProjectionValues;

public class UserCodec implements ModelCodec<User, UserCriteria> {

	@Override
	public void writeEntity(ExtensibleObjectOutput out, User entity) {
		out.beginObject();
		if (entity.id() != null) out.name("id").value(entity.id());
		if (entity.userName() != null) out.name("userName").value(entity.userName());
		if (entity.name() != null) out.name("name").value(entity.name());
		if (entity.password() != null) out.name("password").value(entity.password());
		if (entity.roles() != null) out.name("roles").value(entity.roles());
		out.endObject();
	}

	@Override
	public void writeEntity(ExtensibleObjectOutput out, User entity, EntityGraph graph) {
		if (!graph.track(entity)) {
			out.beginObject();
			if (entity.id() != null) out.name("id").value(entity.id());
			out.endObject();
			return;
		}
		writeEntity(out, entity);
	}

	@Override
	public void writeEntityProjected(ExtensibleObjectOutput out, User entity, User projection) {
		out.beginObject();
		if (entity.id() != null) out.name("id").value(entity.id());
		if (projection.userName() != null) {
			out.name("userName");
			if (entity.userName() != null) out.value(entity.userName()); else out.nullValue();
		}
		if (projection.name() != null) {
			out.name("name");
			if (entity.name() != null) out.value(entity.name()); else out.nullValue();
		}
		if (projection.password() != null) {
			out.name("password");
			if (entity.password() != null) out.value(entity.password()); else out.nullValue();
		}
		if (projection.roles() != null) {
			out.name("roles");
			if (entity.roles() != null) out.value(entity.roles()); else out.nullValue();
		}
		out.endObject();
	}

	@Override
	public User computeProjection(User newEntity, User oldEntity) {
		var pv = ProjectionValues.INSTANCE;
		var projection = new User();
		if (!java.util.Objects.equals(newEntity.userName(), oldEntity.userName())) projection.withUserName(pv.str);
		if (!java.util.Objects.equals(newEntity.name(), oldEntity.name())) projection.withName(pv.str);
		if (!java.util.Objects.equals(newEntity.password(), oldEntity.password())) projection.withPassword(pv.str);
		if (!java.util.Objects.equals(newEntity.roles(), oldEntity.roles())) projection.withRoles(pv.str);
		return projection;
	}

	@Override
	public User readEntity(ExtensibleObjectInput in) {
		var user = new User();
		in.beginObject();
		while (in.hasNext()) {
			switch (in.nextName()) {
				case "id" -> user.withId(InputCoerceUtils.asLong(in));
				case "userName" -> user.withUserName(InputCoerceUtils.asString(in));
				case "name" -> user.withName(InputCoerceUtils.asString(in));
				case "password" -> user.withPassword(InputCoerceUtils.asString(in));
				case "roles" -> user.withRoles(InputCoerceUtils.asString(in));
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
				case "id" -> { entity.withId(InputCoerceUtils.asLong(in)); projection.withId(pv.i64); }
				case "userName" -> { entity.withUserName(InputCoerceUtils.asString(in)); projection.withUserName(pv.str); }
				case "name" -> { entity.withName(InputCoerceUtils.asString(in)); projection.withName(pv.str); }
				case "password" -> { entity.withPassword(InputCoerceUtils.asString(in)); projection.withPassword(pv.str); }
				case "roles" -> { entity.withRoles(InputCoerceUtils.asString(in)); projection.withRoles(pv.str); }
				default -> in.skipValue();
			}
		}
		in.endObject();
		return new UpdateData<>(entity, projection);
	}

	@Override
	public void writeCriteriaFields(ExtensibleObjectOutput out, UserCriteria criteria) {
		CriterionCodec.write(out, "userId", criteria.userId(), CriterionCodec.LONG_OUT);
		CriterionCodec.write(out, "userName", criteria.userName(), CriterionCodec.STRING_OUT);
		CriterionCodec.write(out, "password", criteria.password(), CriterionCodec.STRING_OUT);
		CriterionCodec.write(out, "name", criteria.name(), CriterionCodec.STRING_OUT);
		CriterionCodec.write(out, "roles", criteria.roles(), CriterionCodec.STRING_OUT);
		if (criteria.orderBy() != null) out.name("orderBy").value(criteria.orderBy().name());
	}

	@Override
	public boolean readCriteriaField(ExtensibleObjectInput in, String fieldName, UserCriteria criteria) {
		switch (fieldName) {
			case "userId" -> CriterionCodec.read(in, criteria.userId(), CriterionCodec.LONG_IN);
			case "userName" -> CriterionCodec.read(in, criteria.userName(), CriterionCodec.STRING_IN);
			case "password" -> CriterionCodec.read(in, criteria.password(), CriterionCodec.STRING_IN);
			case "name" -> CriterionCodec.read(in, criteria.name(), CriterionCodec.STRING_IN);
			case "roles" -> CriterionCodec.read(in, criteria.roles(), CriterionCodec.STRING_IN);
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
		entity.withId(id);
	}
}
