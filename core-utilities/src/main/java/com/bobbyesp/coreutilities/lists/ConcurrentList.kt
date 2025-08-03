package com.bobbyesp.coreutilities.lists

import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * A thread-safe implementation of a mutable list using a read-write lock.
 *
 * @param T the type of elements in this list
 */
class ConcurrentList<T> : MutableList<T> {
    private val list = mutableListOf<T>()
    private val lock = ReentrantReadWriteLock()

    /**
     * Returns the number of elements in the list.
     */
    override val size: Int get() = lock.read { list.size }

    /**
     * Replaces the element at the specified position in this list with the specified element.
     *
     * @param index the index of the element to replace
     * @param element the element to be stored at the specified position
     * @return the element previously at the specified position
     */
    override operator fun set(index: Int, element: T) = lock.write { list.set(index, element) }

    /**
     * Returns the element at the specified position in this list.
     *
     * @param index the index of the element to return
     * @return the element at the specified position in this list
     */
    override operator fun get(index: Int) = lock.read { list[index] }

    /**
     * Returns `true` if this list contains the specified element.
     *
     * @param element the element whose presence in this list is to be tested
     * @return `true` if this list contains the specified element
     */
    override fun contains(element: T) = lock.read { list.contains(element) }

    /**
     * Returns `true` if this list contains all of the elements in the specified collection.
     *
     * @param elements collection to be checked for containment in this list
     * @return `true` if this list contains all of the elements in the specified collection
     */
    override fun containsAll(elements: Collection<T>) = lock.read { list.containsAll(elements) }

    /**
     * Returns the index of the first occurrence of the specified element in this list,
     * or -1 if this list does not contain the element.
     *
     * @param element the element to search for
     * @return the index of the first occurrence of the specified element in this list,
     * or -1 if this list does not contain the element
     */
    override fun indexOf(element: T) = lock.read { list.indexOf(element) }

    /**
     * Returns the index of the last occurrence of the specified element in this list,
     * or -1 if this list does not contain the element.
     *
     * @param element the element to search for
     * @return the index of the last occurrence of the specified element in this list,
     * or -1 if this list does not contain the element
     */
    override fun lastIndexOf(element: T) = lock.read { list.lastIndexOf(element) }

    /**
     * Returns `true` if this list contains no elements.
     *
     * @return `true` if this list contains no elements
     */
    override fun isEmpty() = lock.read { list.isEmpty() }

    /**
     * Returns a view of the portion of this list between the specified `fromIndex`, inclusive,
     * and `toIndex`, exclusive.
     *
     * @param fromIndex low endpoint (inclusive) of the subList
     * @param toIndex high endpoint (exclusive) of the subList
     * @return a view of the specified range within this list
     */
    override fun subList(fromIndex: Int, toIndex: Int) =
        lock.read { list.subList(fromIndex, toIndex) }

    /**
     * Appends the specified element to the end of this list.
     *
     * @param element element to be appended to this list
     * @return `true` (as specified by `Collection.add`)
     */
    override fun add(element: T) = lock.write { list.add(element) }

    /**
     * Inserts the specified element at the specified position in this list.
     *
     * @param index index at which the specified element is to be inserted
     * @param element element to be inserted
     */
    override fun add(index: Int, element: T) = lock.write { list.add(index, element) }

    /**
     * Appends all of the elements in the specified collection to the end of this list,
     * in the order that they are returned by the specified collection's iterator.
     *
     * @param elements collection containing elements to be added to this list
     * @return `true` if this list changed as a result of the call
     */
    override fun addAll(elements: Collection<T>) = lock.write { list.addAll(elements) }

    /**
     * Inserts all of the elements in the specified collection into this list,
     * starting at the specified position.
     *
     * @param index index at which to insert the first element from the specified collection
     * @param elements collection containing elements to be added to this list
     * @return `true` if this list changed as a result of the call
     */
    override fun addAll(index: Int, elements: Collection<T>) =
        lock.write { list.addAll(index, elements) }

    /**
     * Removes all of the elements from this list.
     */
    override fun clear() = lock.write { list.clear() }

    /**
     * Removes the first occurrence of the specified element from this list, if it is present.
     *
     * @param element element to be removed from this list, if present
     * @return `true` if this list contained the specified element
     */
    override fun remove(element: T) = lock.write { list.remove(element) }

    /**
     * Removes from this list all of its elements that are contained in the specified collection.
     *
     * @param elements collection containing elements to be removed from this list
     * @return `true` if this list changed as a result of the call
     */
    override fun removeAll(elements: Collection<T>) = lock.write { list.removeAll(elements) }

    /**
     * Removes the element at the specified position in this list.
     *
     * @param index the index of the element to be removed
     * @return the element that was removed from the list
     */
    override fun removeAt(index: Int) = lock.write { list.removeAt(index) }

    /**
     * Retains only the elements in this list that are contained in the specified collection.
     *
     * @param elements collection containing elements to be retained in this list
     * @return `true` if this list changed as a result of the call
     */
    override fun retainAll(elements: Collection<T>) = lock.write { list.retainAll(elements) }

    /**
     * Returns an iterator over the elements in this list in proper sequence.
     *
     * @return an iterator over the elements in this list in proper sequence
     */
    override fun iterator() = lock.write { list.iterator() }

    /**
     * Returns a list iterator over the elements in this list (in proper sequence).
     *
     * @return a list iterator over the elements in this list (in proper sequence)
     */
    override fun listIterator() = lock.write { list.listIterator() }

    /**
     * Returns a list iterator over the elements in this list (in proper sequence),
     * starting at the specified position in the list.
     *
     * @param index index of the first element to be returned from the list iterator
     * @return a list iterator over the elements in this list (in proper sequence),
     * starting at the specified position in the list
     */
    override fun listIterator(index: Int) = lock.write { list.listIterator(index) }
}