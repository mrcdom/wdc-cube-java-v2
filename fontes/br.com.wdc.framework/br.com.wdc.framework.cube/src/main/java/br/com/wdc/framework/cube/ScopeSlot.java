package br.com.wdc.framework.cube;

/**
 * Where a child presenter shows itself in its parent: it hands over its scope, and the parent puts it in one of its
 * props ({@code content}). Which view draws it is up to the view layer ({@link ScopeViews}).
 */
public interface ScopeSlot {

    void setScope(Scope scope);

}
