
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ConcurrentSearcherList<T> {

    int numSearchers, numInserters, numRemovers;
    final Lock listLock;
    final Condition insertCond, searchCond, removeCond;

	/*
	 * Three kinds of threads share access to a singly-linked list:
	 * searchers, inserters and deleters. Searchers merely examine the list;
	 * hence they can execute concurrently with each other. Inserters add
	 * new items to the front of the list; insertions must be mutually exclusive
	 * to preclude two inserters from inserting new items at about
	 * the same time. However, one insert can proceed in parallel with
	 * any number of searches. Finally, deleters remove items from anywhere
	 * in the list. At most one deleter process can access the list at
	 * a time, and deletion must also be mutually exclusive with searches
	 * and insertions.
	 *
	 * Make sure that there are no data races between concurrent inserters and searchers!
	 */

    private static class Node<T>{
        final T item;
        Node<T> next;

        Node(T item, Node<T> next){
            this.item = item;
            this.next = next;
        }
    }


    private volatile Node<T> first;



    public ConcurrentSearcherList() {
        first = null;
        numSearchers = 0;
        numInserters = 0;
        numRemovers = 0;
        listLock = new ReentrantLock();
        insertCond = listLock.newCondition();
        removeCond = listLock.newCondition();
        searchCond = listLock.newCondition();

    }

    /**
     * Inserts the given item into the list.
     *
     * Precondition:  item != null
     *
     * @param item
     * @throws InterruptedException
     */
    public void insert(T item) throws InterruptedException{
        assert item != null: "Error in ConcurrentSearcherList insert:  Attempt to insert null";
        start_insert();
        try{
            first = new Node<T>(item, first);
        }
        finally{
            end_insert();
        }
    }

    /**
     * Determines whether or not the given item is in the list
     *
     * Precondition:  item != null
     *
     * @param item
     * @return  true if item is in the list, false otherwise.
     * @throws InterruptedException
     */
    public boolean search(T item) throws InterruptedException{
        assert item != null: "Error in ConcurrentSearcherList insert:  Attempt to search for null";
        start_search();
        try{
            for(Node<T> curr = first;  curr != null ; curr = curr.next){
                if (item.equals(curr.item)) return true;
            }
            return false;
        }
        finally{
            end_search();
        }
    }

    /**
     * Removes the given item from the list if it exists.  Otherwise the list is not modified.
     * The return value indicates whether or not the item was removed.
     *
     * Precondition:  item != null.
     *
     * @param item
     * @return  whether or not item was removed from the list.
     * @throws InterruptedException
     */
    public boolean remove(T item) throws InterruptedException{
        assert item != null: "Error in ConcurrentSearcherList insert:  Attempt to remove null";
        start_remove();
        try{
            if(first == null) return false;
            if (item.equals(first.item)){first = first.next; return true;}
            for(Node<T> curr = first;  curr.next != null ; curr = curr.next){
                if (item.equals(curr.next.item)) {
                    curr.next = curr.next.next;
                    return true;
                }
            }
            return false;
        }
        finally{
            end_remove();
        }
    }

    private void start_insert() throws InterruptedException{
        listLock.lock();
        try{
            while(numRemovers != 0 || numInserters != 0)
                insertCond.await();
            numInserters++;
        }finally {
            listLock.unlock();
        }
    }

    private void end_insert(){
        listLock.lock();
        try{
            numInserters--;
            if(numSearchers == 0)
                removeCond.signal();
            else
                insertCond.signal();
        }finally {
            listLock.unlock();
        }
    }

    private void start_search() throws InterruptedException{
        listLock.lock();
        try{
            while(numRemovers != 0)
                searchCond.await();
            numSearchers++;
        }finally {
            listLock.unlock();
        }
    }

    private void end_search(){
        listLock.lock();
        try{
            numSearchers--;
            if(numRemovers == 0 && numSearchers == 0 && numInserters == 0)
                removeCond.signal();
        }finally {
            listLock.unlock();
        }
    }

    private void start_remove() throws InterruptedException{
        listLock.lock();

        try{
            while(numInserters != 0 || numSearchers !=0 || numRemovers !=0)
                removeCond.await();
            numRemovers++;
        }finally {
            listLock.unlock();
        }
    }

    private void end_remove() {
        listLock.lock();

        try{
            numRemovers--;

            insertCond.signal();
            searchCond.signalAll();
            removeCond.signal();

        }finally {
            listLock.unlock();
        }
    }
}
