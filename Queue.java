/*
 * Created on Feb 2, 2005
 */

import java.util.*;

/**
 * Thread safe object queue. Wrapper for LinkedList.
 * @author jle
 */
public class Queue {
    private LinkedList list = new LinkedList();
    
    /**
     * Appends the given element to the end of this queue. 
     * @param o - the element to be inserted at the end of this queue.
     */
    public synchronized void addLast(Object o) {
        list.addLast(o);
        notify();
    }
    
    /**
     * Removes and returns the first element from this queue.
     * Blocks if there are no items in the queue
     * @return the first element from this queue.
     * @throws InterruptedException
     */
    public synchronized Object removeFirst() throws InterruptedException {
        while (list.isEmpty())
            wait();
        return list.removeFirst();
    }
    
    public int size() {
        return list.size();
    }
}
