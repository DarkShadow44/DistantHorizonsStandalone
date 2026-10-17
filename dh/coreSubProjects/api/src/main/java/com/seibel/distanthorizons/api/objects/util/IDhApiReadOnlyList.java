package com.seibel.distanthorizons.api.objects.util;

/**
 * @since API 7.3.0
 * @version 2026-10-02
 */
public interface IDhApiReadOnlyList<T>
{
	T get(int index);
	int size();
	
}
