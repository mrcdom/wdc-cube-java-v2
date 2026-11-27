package br.com.wdc.cube.backend;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** A request from a page served by this same server is not cross-origin, at whatever address it is reached. */
public class SameOriginFilterTest {

    @Test
    public void theSameHostAndPort_isTheSameOrigin() {
        assertTrue(SameOriginFilter.isSameOrigin("http://localhost:18080", "localhost:18080"));
        assertTrue(SameOriginFilter.isSameOrigin("https://store.example.com", "store.example.com"));
        assertTrue(SameOriginFilter.isSameOrigin("https://store.example.com", "Store.Example.com:443"));
        assertTrue(SameOriginFilter.isSameOrigin("http://store.example.com:80", "store.example.com"));
    }

    @Test
    public void anotherHostOrPort_isLeftToTheCorsRules() {
        assertFalse(SameOriginFilter.isSameOrigin("http://localhost:3000", "localhost:18080"));
        assertFalse(SameOriginFilter.isSameOrigin("http://evil.example.com:18080", "localhost:18080"));
        assertFalse(SameOriginFilter.isSameOrigin("https://store.example.com", "store.example.com:80"));
        assertFalse(SameOriginFilter.isSameOrigin("null", "localhost:18080"));
        assertFalse(SameOriginFilter.isSameOrigin("tauri://localhost", "localhost"));
        assertFalse(SameOriginFilter.isSameOrigin(null, "localhost:18080"));
        assertFalse(SameOriginFilter.isSameOrigin("http://localhost:18080", null));
    }
}
