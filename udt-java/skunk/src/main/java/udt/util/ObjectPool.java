/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package udt.util;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A non blocking object pool which allows objects to be cleaned and recycled
 * before reuse.
 * 
 * This is intended for mutable objects that would normally be created by one thread,
 * handed to another thread, then recycled.  The recycle operation enables the
 * second thread to determine when it has finished with the object.
 * 
 * Concurrent thread safe.
 * 
 * @author peter
 */
public class ObjectPool<T> extends Recycler<T>{
    private final Queue<T> queue;
    private final AtomicInteger length;
    private volatile int max; // approximate max queue size.

    public ObjectPool(int maxPoolSize){
        max = maxPoolSize;
        queue = new ConcurrentLinkedQueue<T>();
        length = new AtomicInteger();
    }
    
    /**
     * Get an object from the pool.
     * @return - null if empty.
     */
    public T get() {
        return queue.poll();
    }

    @Override
    protected void recycled(T r) {
        if (length.get() < max){
            queue.offer(r);
        }
    }
}
