package br.com.wdc.framework.cube;

import java.io.StringWriter;

import com.google.gson.stream.JsonWriter;

import br.com.wdc.framework.commons.gson.JsonExtensibleObjectOutput;
import br.com.wdc.framework.commons.serialization.ExtensibleObjectOutput;

public interface ViewState {

    void write(String instanceId, ExtensibleObjectOutput json);

    default String toJson(String instanceId) {
        var strWriter = new StringWriter();
        var json = new JsonExtensibleObjectOutput(new JsonWriter(strWriter));
        try {
            this.write(instanceId, json);
        } finally {
            json.flush();
        }

        return strWriter.toString();
    }

}
