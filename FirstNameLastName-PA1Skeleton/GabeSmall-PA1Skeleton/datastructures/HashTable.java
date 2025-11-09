package datastructures;

import main.Edge;
import main.Vertex;

/**
 * HashTable is a custom implementation of a HashMap that supports either Vertex, Edge, or String as keys
 */
public class HashTable<K, V> {

    private static class Entry<K, V> {
        K key;
        V value;
        boolean isDeleted;

        Entry(K key, V value) {
            this.key = key;
            this.value = value;
            this.isDeleted = false;
        }
    }

    private Entry<K, V>[] table;
    private int size;
    private static final double LOAD_FACTOR = 0.5;
    private static final int DEFAULT_CAPACITY = 16;

    /**
     * Initializes an instance of HashTable
     */
    @SuppressWarnings("unchecked")
    public HashTable() {
        this.table = (Entry<K, V>[]) new Entry[DEFAULT_CAPACITY];
        this.size = 0;
    }

    /**
     * @param key the object being hashed
     * @return the hashed result modded by size of the internal array
     */
    private int hash(K key) {
        if (key instanceof String) {
            String s = (String) key;
            int h = 0;
            for (int i = 0; i < s.length(); i++) {
                h *= 79;
                h += s.charAt(i) % 79;
                h %= table.length;
            }
            return h;
        } else if (key instanceof Vertex) {
            return ((Vertex) key).getId() % table.length;
        } else if (key instanceof Edge) {
            return ((Edge) key).getId() % table.length;
        }
        return Math.abs(key.hashCode()) % table.length;
    }

    /**
     * @param key the object being hashed
     * @return the index of the key based on the hash and linear probing 
     */
    private int getIndex(K key) {
        int index = hash(key);
        int deleted = -1;
        for (int i = 0; i < table.length && table[index] != null; i++) {
            if (table[index].key.equals(key)) {
                return index;
            } else if (table[index].isDeleted && deleted == -1) {
                deleted = index;
            }
            index = (index+1) % table.length;
        }
        return deleted == -1 ? index : deleted;
    }

    /**
     * @param key the object being hashed
     * @param value the value associated with the key
     */
    public void put(K key, V value) {
        if (key == null) return;
        if ((double) size / table.length >= LOAD_FACTOR) resize();

        int index = getIndex(key);
        if (table[index] == null || table[index].isDeleted) size++; 
        table[getIndex(key)] = new Entry<>(key, value);
    }

    /**
     * @param key an object in the HashTable
     * @return the value of the key
     */
    public V get(K key) {
        if (key == null) return null;
        int index = getIndex(key);
        if (table[index] == null || table[index].isDeleted) return null;
        return table[index].value;
    }

    /**
     * @param key an object in the HashTable
     * @return the removed object
     */
    public V remove(K key) {
        if (key == null) return null;
        int index = getIndex(key);
        Entry<K, V> entry = table[index];
        if (entry != null) {
            if (!entry.isDeleted) size--;
            entry.isDeleted = true;
            return entry.value;
        }
        return null;
    }

    /**
     * @param key non-null object
     * @return whether the key is inside the HashTable
     */
    public boolean containsKey(K key) {
        return get(key) != null;
    }

    @SuppressWarnings("unchecked")
    private void resize() {
        Entry<K, V>[] prevTable = table;
        table = (Entry<K, V>[]) new Entry[table.length * 2];
        for (Entry<K, V> entry : prevTable) {
            if (entry != null && !entry.isDeleted) {
                put(entry.key, entry.value);
            }
        }
    }
}