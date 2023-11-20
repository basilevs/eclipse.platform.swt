package org.eclipse.swt.widgets;

import java.util.*;

/**
 * @since 3.125
 */
final class IntMap<T> {
	private final ArrayList<T> entries = new ArrayList<>();
	private int nextKey = 0;

	public T get(int key) {
		if (key < 0 || key >= entries.size()) return null;
		return entries.get(key);
	}

	public T put(int key, T value) {
		if (value == null) throw new NullPointerException();
		ensureSize(key + 1);
		return entries.set(key, value);
	}

	public int findAbsentKey() {
		int start = nextKey;
		for (int i = start; i < entries.size(); i++) {
			if (entries.get(i) == null) {
				return nextKey = i;
			}
		}
		if (start < entries.size()) {
			for (int i = 0; i < start ; i++) {
				if (entries.get(i) == null) {
					return nextKey = i;
				}
			}
		}
		return nextKey = entries.size();
	}

	private void ensureSize(int size) {
		if (size < 0) throw new IllegalArgumentException("Invalid size: " + size);
		int oldSize = entries.size();
		if (size <= oldSize) return;
		// '.setRedraw(false)' is typically used during bulk operations.
		// Reallocate to 1.5x the old size to avoid frequent reallocations.
		int extra = (size + 1) / 2 * 3;
		entries.addAll(Collections.nCopies(extra, null));
	}

	public Iterable<T>  values() {
		return () -> entries.stream().filter(Objects::nonNull).iterator();
	}

	public T remove(int key) {
		nextKey = key;
		return entries.set(key, null);
	}

	public void clear() {
		entries.clear();
	}
}
