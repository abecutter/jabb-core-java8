/**
 * 
 */
package net.sf.jabb.util.col;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * A wrapper of Map that always automatically do computeIfAbsent(...) within get(...) 
 * @author James Hu
 *
 */
public class ComputeIfAbsentMap<M extends Map<K, V>, K, V> implements Map<K, V>, Serializable{
	private static final long serialVersionUID = 1154008525294057715L;
	
	protected M map;
	protected Function<? super K,? extends V> computeFunction;

	
    /**
     * Creates a new map with the same mappings as the given map.
     *
     * @param m the map
     * @param computeFunction the function to compute a value when there is no value currently associated with the key
     */
	public ComputeIfAbsentMap(M m, Function<? super K,? extends V> computeFunction){
		this.map = m;
		this.computeFunction = computeFunction;
	}
	
	public M getMap(){
		return map;
	}

	@Override
	public int size() {
		return map.size();
	}

	@Override
	public boolean isEmpty() {
		return map.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return map.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return map.containsValue(value);
	}

	@SuppressWarnings("unchecked")
	@Override
	public V get(Object key) {
    	return map.computeIfAbsent((K)key, computeFunction);
	}

	@Override
	public V put(K key, V value) {
		return map.put(key, value);
	}

	@Override
	public V remove(Object key) {
		return map.remove(key);
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		map.putAll(m);
	}

	@Override
	public void clear() {
		map.clear();
	}

	@Override
	public Set<K> keySet() {
		return map.keySet();
	}

	@Override
	public Collection<V> values() {
		return map.values();
	}

	@Override
	public Set<java.util.Map.Entry<K, V>> entrySet() {
		return map.entrySet();
	}
	
	/////// overriding defaults //////////
	
	@Override
	public V getOrDefault(Object key, V defaultValue) {
		return map.getOrDefault(key, defaultValue);
	}
	
	@Override
	public void forEach(BiConsumer<? super K, ? super V> action) {
		map.forEach(action);
	}
	
	@Override
	public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
		map.replaceAll(function);
	}
	
	@Override
	public V putIfAbsent(K key, V value) {
		return map.putIfAbsent(key, value);
	}

	@Override
	public boolean remove(Object key, Object value) {
		return map.remove(key, value);
	}
	
	@Override
	public boolean replace(K key, V oldValue, V newValue) {
		return map.replace(key, oldValue, newValue);
	}
	
	@Override
	public V replace(K key, V value) {
		return map.replace(key, value);
	}
	
	@Override
	public V computeIfAbsent(K key,
            Function<? super K, ? extends V> mappingFunction) {
		return map.computeIfAbsent(key, mappingFunction);
	}
	
	@Override
	public V computeIfPresent(K key,
            BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
		return map.computeIfPresent(key, remappingFunction);
	}
	
	@Override
	public V compute(K key,
            BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
		return map.compute(key, remappingFunction);
	}
	
	@Override
	public V merge(K key, V value,
            BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
		return map.merge(key, value, remappingFunction);
	}
}
