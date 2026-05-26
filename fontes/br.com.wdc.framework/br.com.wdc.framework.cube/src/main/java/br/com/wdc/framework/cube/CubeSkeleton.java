package br.com.wdc.framework.cube;

import java.util.Map;

public interface CubeSkeleton {
    
    String classId();

	void submit(int eventCode, int eventQtde, Map<String, Object> formData) throws Exception;

	default void syncState(Map<String, Object> formData) {
		// NOOP
	}

}
