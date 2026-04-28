package br.com.wdc.framework.commons.serialization;

import java.io.IOException;

public interface Externalizable {

	void writeExternal(ExtensibleObjectOutput out) throws IOException;

	void readExternal(ExtensibleObjectInput in) throws IOException;

}
