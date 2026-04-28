package br.com.wdc.framework.commons.gson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.NotImplementedException;

import com.google.gson.stream.JsonReader;

import br.com.wdc.framework.commons.function.ThrowingConsumer;
import br.com.wdc.framework.commons.function.ThrowingFunction;
import br.com.wdc.framework.commons.function.ThrowingRunnable;

public class JsonReaderHelper {

    protected JsonReader jr;

    public JsonReaderHelper(JsonReader jr) {
        this.jr = jr;
    }

    public void notImplemented() {
        throw new NotImplementedException();
    }

    public void object(ThrowingConsumer<Map<String, ThrowingRunnable>> actions) throws IOException {
        Map<String, ThrowingRunnable> actionMap = new HashMap<>();
        actions.accept(actionMap);

        jr.beginObject();
        while (jr.hasNext()) {
            String name = jr.nextName();
            ThrowingRunnable action = actionMap.get(name);
            if (action != null) {
                action.run();
                continue;
            }
            jr.skipValue();
        }
        jr.endObject();
    }

    public <E> Set<E> arrayAsSet(ThrowingFunction<JsonReader, E> parseJson) throws IOException {
        Set<E> itens = new LinkedHashSet<>();
        jr.beginArray();
        while (jr.hasNext()) {
            itens.add(parseJson.apply(jr));
        }
        jr.endArray();
        return itens;
    }

    public <E> List<E> arrayAsList(ThrowingFunction<JsonReader, E> parseJson) throws IOException {
        List<E> itens = new ArrayList<>();
        jr.beginArray();
        while (jr.hasNext()) {
            itens.add(parseJson.apply(jr));
        }
        jr.endArray();
        return itens;
    }

}
