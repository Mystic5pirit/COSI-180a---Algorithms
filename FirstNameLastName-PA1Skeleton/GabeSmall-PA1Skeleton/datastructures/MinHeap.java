package datastructures;

/**
 * MinHeap datastructure that supports the implementation of Dijkstra's Algorithm, Prim's MST algorithm, and Kruskal's MST algorithm
 */
public class MinHeap<K> {
    
    public static class Entry<K> {
        private K key;
        private Double priority;

        private Entry(K key, Double priority) {
            this.key = key;
            this.priority = priority;
        }

        public K getKey() {
            return key;
        }

        public Double getPriority() {
            return priority;
        }
    }

    private Entry<K>[] heap;
    private HashTable<K, Integer> index;
    private int size;
    private static final int DEFAULT_CAPACITY = 16;

    /**
     * Initializes an instance of MinHeap with a default capacity of 16
     */
    @SuppressWarnings("unchecked")
    public MinHeap() {
        this.heap = (Entry<K>[]) new Entry[DEFAULT_CAPACITY];
        this.index = new HashTable<>();
        this.size = 0;
    }

    /**
     * @return the number of entrys in the MinHeap
     */
    public int size() {
        return size;
    }
    
    /**
     * Adds the key to the MinHeap only if the key is not already added
     * @param key the object that is inserted into the MinHeap
     * @param priority the associated priority for the key
     */
    public void add(K key, Double priority) {
        if (!index.containsKey(key)) {
            index.put(key, size);
            heap[size++] = new Entry<>(key, priority);
            heapifyUp(key);
            if (size == heap.length) resize();
        } 
    }

    /**
     * Removes the key with the smallest priority in the MinHeap
     * @return the removed Entry that holds the key and priority
     */
    public Entry<K> poll() {
        if (size <= 0) return null;
        if (size == 1) {
            Entry<K> result = heap[--size];
            index.remove(result.key);
            return result;
        }
        Entry<K> result = heap[0];
        swap(0, --size);
        index.remove(result.key);
        heapifyDown(heap[0].key);
        return result;
    }

    /**
     * @return an Entry of the key with the smallest priority in the MinHeap
     */
    public Entry<K> peek() {
        return heap[0];
    }

    /**
     * @param key any key that is contained in the MinHeap
     * @return the associated priority of the key in the MinHeap
     */
    public Double getPriority(K key) {
        if (!index.containsKey(key)) return null;
        return heap[index.get(key)].priority;
    }

    /**
     * Updates the priority for a key that is contained in the MinHeap
     * @param key any key that is contained in the MinHeap
     * @param priority any non-null Double value
     */
    public void update(K key, Double newPriority) {
        if (!index.containsKey(key) || newPriority == null) return;
        int i = index.get(key);
        heap[i].priority = newPriority;
        heapifyUp(key);
        heapifyDown(key);
    }

    /**
     * @param key  
     * @return whether the key is in the MinHeap
     */
    public boolean containsKey(K key) {
        return index.containsKey(key);
    }

    /**
     * Restores the heap property by continuously swapping with the key's parent
     * @param key the key which violates the heap property
     */
    private void heapifyUp(K key) {
        int i = index.get(key);
        int parent_i = (i-1)/2;
        while (parent_i >= 0 && heap[parent_i].priority > heap[i].priority) {
            swap(i, parent_i);
            i = parent_i;
            parent_i = (i-1)/2;
        }
    }

    /**
     * Restores the heap property by continuosly swapping with the key's children
     * @param key the key which violates the heap property
     */
    private void heapifyDown(K key) {
        int i = index.get(key);
        int left = 2*i + 1;
        int right = 2*i + 2;
        int largest = i;
        if (left < size && heap[largest].priority > heap[left].priority)
            largest = left;
        if (right < size && heap[largest].priority > heap[right].priority)
            largest = right;
        if (largest != i) {
            swap(largest, i);
            heapifyDown(key);
        }
    }

    private void swap(int i, int j) {
        Entry<K> temp = heap[i];
        heap[i] = heap[j];
        heap[j] = temp;
        index.put(heap[i].key, i);
        index.put(heap[j].key, j);
    }

    @SuppressWarnings("unchecked")
    private void resize() {
        Entry<K>[] prevHeap = heap;
        heap = (Entry<K>[]) new Entry[heap.length * 2];
        for (int i=0; i<prevHeap.length; i++) {
            heap[i] = prevHeap[i];
        }
    }
}
