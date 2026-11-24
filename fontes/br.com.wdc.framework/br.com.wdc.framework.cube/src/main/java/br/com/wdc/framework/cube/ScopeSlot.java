package br.com.wdc.framework.cube;

/**
 * Where a child presenter shows itself in its parent: it hands over its scope, and the parent puts it in one of its
 * props ({@code content}). Drawing it is up to the rendering technology.
 */
public interface ScopeSlot {

    void setScope(Scope scope);

}
