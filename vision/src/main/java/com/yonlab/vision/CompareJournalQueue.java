package com.yonlab.vision;

import java.util.ArrayDeque;

/** Memory-only ownership/coalescing policy. No callback or I/O runs while this monitor is held. */
final class CompareJournalQueue<T> {
    static final class Item<T> {final long owner,revision;final T value;Item(long owner,long revision,T value){this.owner=owner;this.revision=revision;this.value=value;}}
    private final int capacity;
    private final ArrayDeque<Item<T>> events=new ArrayDeque<>();
    private Item<T> latest;
    private long owner,revision,dropped,discarded;
    CompareJournalQueue(int capacity){if(capacity<1)throw new IllegalArgumentException("event capacity");this.capacity=capacity;}
    synchronized void claim(long next){if(next<=owner)return;discarded+=events.size()+(latest==null?0:1);events.clear();latest=null;owner=next;dropped=0;}
    synchronized boolean state(long request,T value){if(request!=owner)return false;latest=new Item<>(request,++revision,value);return true;}
    synchronized boolean event(long request,T value){if(request!=owner)return false;if(events.size()>=capacity){dropped++;return false;}events.addLast(new Item<>(request,++revision,value));return true;}
    synchronized Item<T> takeState(){Item<T> value=latest;latest=null;return value;}
    synchronized Item<T> pollEvent(){return events.pollFirst();}
    synchronized boolean isCurrent(long request){return request==owner;}
    synchronized boolean hasWork(){return latest!=null||!events.isEmpty();}
    synchronized long droppedEvents(){return dropped;}
    synchronized long staleQueuedDiscarded(){return discarded;}
}
