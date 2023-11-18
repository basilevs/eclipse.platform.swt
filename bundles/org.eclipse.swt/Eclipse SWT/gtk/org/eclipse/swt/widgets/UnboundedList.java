package org.eclipse.swt.widgets;

import java.util.*;

/**
 * @since 3.125
 */
class UnboundedList<E> {
	private final ArrayList<E> elements = new ArrayList<>();
	private static final long serialVersionUID = 2094080429346251262L;
	public int size() {
		return elements.size();
	}



}
