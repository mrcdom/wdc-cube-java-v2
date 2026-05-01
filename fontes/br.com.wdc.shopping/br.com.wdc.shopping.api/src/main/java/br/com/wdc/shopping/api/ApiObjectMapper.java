package br.com.wdc.shopping.api;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;

import br.com.wdc.shopping.domain.model.Purchase;

/**
 * ObjectMapper compartilhado para os endpoints REST da API de repositório.
 *
 * Configurações: - OffsetDateTime serializado como ISO-8601 string - PurchaseItem.purchase ignorado para evitar referência circular - Product.image ignorado
 * (usar endpoint dedicado /api/repo/product/{id}/image) - NON_NULL: campos null não são serializados
 */
public final class ApiObjectMapper {

	private ApiObjectMapper() {
	}

	private static final ObjectMapper INSTANCE = createMapper();

	public static ObjectMapper get() {
		return INSTANCE;
	}

	private static ObjectMapper createMapper() {
		var mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

		var timeModule = new SimpleModule("JavaTimeModule");
		timeModule.addSerializer(OffsetDateTime.class, new JsonSerializer<>() {
			@Override
			public void serialize(OffsetDateTime value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
				gen.writeString(value.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
			}
		});
		timeModule.addDeserializer(OffsetDateTime.class, new JsonDeserializer<>() {
			@Override
			public OffsetDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
				return OffsetDateTime.parse(p.getText(), DateTimeFormatter.ISO_OFFSET_DATE_TIME);
			}
		});
		mapper.registerModule(timeModule);

		mapper.addMixIn(br.com.wdc.shopping.domain.model.PurchaseItem.class, PurchaseItemMixin.class);
		mapper.addMixIn(br.com.wdc.shopping.domain.model.Product.class, ProductMixin.class);

		return mapper;
	}

	/**
	 * Deserializa o campo "projection" do body JSON para o tipo de entidade informado. Retorna null se o campo não estiver presente — nesse caso o controller
	 * usa o default.
	 * 
	 * @throws IllegalArgumentException
	 * @throws JsonProcessingException
	 */
	public static <T> T parseProjection(JsonNode body, Class<T> type) throws JsonProcessingException, IllegalArgumentException {
		if (body.has("projection") && !body.get("projection").isNull()) {
			return get().treeToValue(body.get("projection"), type);
		}
		return null;
	}

	abstract static class PurchaseItemMixin {
		@JsonIgnore
		public Purchase purchase;
	}

	abstract static class ProductMixin {
		@JsonIgnore
		public byte[] image;
	}
}
