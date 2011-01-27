package org.nhahtdh;

import java.util.*;

public class HeapList<T> {
	//---------------
	// Data members
	//---------------
	private LinkedList<LinkedList<T>> heapList;
	private int nextIndex;
	
	//----------------
	// Constructors
	//----------------
	/**
	 * Create a priority queue with the given number of levels.
	 * @param numLevels
	 *        Number of levels in the priority queue.
	 */
	public HeapList(int numLevels) {
		this.heapList = new LinkedList<LinkedList<T>>();
		for (int i = 0; i < numLevels; i++)
			this.heapList.add(new LinkedList<T>());
		this.nextIndex = numLevels;
	}
	
	//----------------
	// Mutators
	//----------------
	public void add(T element, int level) {
		if (level < heapList.size() && level >= 0) {
			this.heapList.get(level).add(element);
			this.nextIndex = Math.min(this.nextIndex, level);
		}
		else throw new IndexOutOfBoundsException("Index out of bounds: " + level);
	}
	
	public void addFirst(T element, int level) {
		if (level < heapList.size() && level >= 0) {
			this.heapList.get(level).addFirst(element);
			this.nextIndex = Math.min(this.nextIndex, level);
		}
		else throw new IndexOutOfBoundsException("Index out of bounds: " + level);
	}
	
	public void addAll(List<T> elementList, int level) {
		if (level < heapList.size() && level >= 0) {
			this.heapList.get(level).addAll(elementList);
			this.nextIndex = Math.min(this.nextIndex, level);
		}
		else throw new IndexOutOfBoundsException("Index out of bounds: " + level);
	}
	
	public T poll() {
		if (nextIndex != heapList.size()) {
			T ret = heapList.get(nextIndex).poll();
			for ( ; nextIndex < heapList.size() && heapList.get(nextIndex).isEmpty(); nextIndex++);
			return ret;
		}
		else
			return null;
	}
	
	public void clear() {
		for (int i = 0; i < heapList.size(); i++)
			heapList.get(i).clear();
		this.nextIndex = heapList.size();
	}
	
	//----------------
	// Accessors
	//----------------
	public int size(int level) {
		if (level < heapList.size() && level >= 0)
			return heapList.get(level).size();
		else throw new IndexOutOfBoundsException("Index out of bounds: " + level);
	}
	
	public int getNextPollIndex() {
		return this.nextIndex == this.heapList.size() ? -1 : this.nextIndex;
	}
	
	public boolean isEmpty() {
		return nextIndex == heapList.size();
	}
	
	public String toString(int level) {
		if (level < heapList.size() && level >= 0)
			return heapList.get(level).toString();
		else throw new IndexOutOfBoundsException("Index out of bounds: " + level);
	}
}
