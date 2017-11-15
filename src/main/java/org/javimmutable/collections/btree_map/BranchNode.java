package org.javimmutable.collections.btree_map;

import org.javimmutable.collections.Cursor;
import org.javimmutable.collections.Holder;
import org.javimmutable.collections.Holders;
import org.javimmutable.collections.JImmutableMap;
import org.javimmutable.collections.SplitableIterator;
import org.javimmutable.collections.Tuple2;
import org.javimmutable.collections.common.ArrayHelper;
import org.javimmutable.collections.cursors.LazyMultiCursor;
import org.javimmutable.collections.indexed.IndexedArray;
import org.javimmutable.collections.iterators.LazyMultiIterator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.Comparator;

@Immutable
public class BranchNode<K, V>
    implements Node<K, V>,
               ArrayHelper.Allocator<Node<K, V>>
{
    private final Node<K, V>[] children;
    private final K baseKey;
    private final int childCount;

    BranchNode(@Nonnull Node<K, V> child1,
               @Nonnull Node<K, V> child2)
    {
        children = allocate(2);
        children[0] = child1;
        children[1] = child2;
        baseKey = child1.baseKey();
        childCount = 2;
    }

    BranchNode(@Nonnull Node<K, V>[] children)
    {
        this.children = children;
        this.baseKey = children[0].baseKey();
        this.childCount = children.length;
    }

    @Nullable
    @Override
    public K baseKey()
    {
        return baseKey;
    }

    @Override
    public int childCount()
    {
        return childCount;
    }

    @Override
    public int valueCount()
    {
        int answer = 0;
        for (Node<K, V> child : children) {
            answer += child.valueCount();
        }
        return answer;
    }

    @Nonnull
    @Override
    public Holder<V> find(@Nonnull Comparator<K> comparator,
                          @Nonnull K key)
    {
        final Node<K, V>[] children = this.children;
        final int index = findChildIndex(comparator, key, children, -1);
        return (index >= 0) ? children[index].find(comparator, key) : Holders.of();
    }

    @Nonnull
    @Override
    public Holder<JImmutableMap.Entry<K, V>> findEntry(@Nonnull Comparator<K> comparator,
                                                       @Nonnull K key)
    {
        final Node<K, V>[] children = this.children;
        final int index = findChildIndex(comparator, key, children, -1);
        return (index >= 0) ? children[index].findEntry(comparator, key) : Holders.of();
    }

    @Nonnull
    @Override
    public UpdateResult<K, V> assign(@Nonnull Comparator<K> comparator,
                                     @Nonnull K key,
                                     V value)
    {
        final Node<K, V>[] children = this.children;
        final int index = findChildIndex(comparator, key, children, 0);
        final UpdateResult<K, V> childResult = children[index].assign(comparator, key, value);
        switch (childResult.type) {
        case UNCHANGED:
            return childResult;

        case INPLACE: {
            final Node<K, V>[] newChildren = ArrayHelper.assign(children, index, childResult.newNode);
            return UpdateResult.createInPlace(new BranchNode<>(newChildren), childResult.sizeDelta);
        }

        case SPLIT: {
            final Node<K, V>[] newChildren = ArrayHelper.assignInsert(this, children, index, childResult.newNode, childResult.extraNode);
            final int newChildCount = newChildren.length;
            if (newChildCount <= MAX_CHILDREN) {
                return UpdateResult.createInPlace(new BranchNode<>(newChildren), childResult.sizeDelta);
            } else {
                final Node<K, V> newChild1 = new BranchNode<>(ArrayHelper.subArray(this, newChildren, 0, MIN_CHILDREN));
                final Node<K, V> newChild2 = new BranchNode<>(ArrayHelper.subArray(this, newChildren, MIN_CHILDREN, newChildCount));
                return UpdateResult.createSplit(newChild1, newChild2, childResult.sizeDelta);
            }
        }

        default:
            throw new IllegalStateException("unknown UpdateResult.Type value");
        }
    }


    @Nonnull
    @Override
    public Node<K, V> delete(@Nonnull Comparator<K> comparator,
                             @Nonnull K key)
    {
        final Node<K, V>[] children = this.children;
        final int index = findChildIndex(comparator, key, children, -1);
        if (index < 0) {
            return this;
        }

        final Node<K, V> child = children[index];
        final Node<K, V> newChild = child.delete(comparator, key);
        if (newChild == child) {
            return this;
        }

        final int childCount = this.childCount;
        final int newChildCount = newChild.childCount();
        if (newChildCount >= MIN_CHILDREN) {
            return new BranchNode<>(ArrayHelper.assign(children, index, newChild));
        } else if (newChildCount == 0) {
            if (childCount == 1) {
                return EmptyNode.of();
            } else {
                return new BranchNode<>(ArrayHelper.delete(this, children, index));
            }
        } else if (childCount == 1) {
            // special case for the root
            return new BranchNode<>(ArrayHelper.assign(children, index, newChild));
        } else {
            Node<K, V> mergeChild;
            Node<K, V> nextChild;
            int mergeIndex;
            if (index == (childCount - 1)) {
                // can't merge at the end of the array
                mergeIndex = index - 1;
                mergeChild = children[mergeIndex];
                nextChild = newChild;
            } else {
                mergeIndex = index;
                mergeChild = newChild;
                nextChild = children[index + 1];
            }
            if ((mergeChild.childCount() + nextChild.childCount()) <= MAX_CHILDREN) {
                final Node<K, V> newMergeChild = mergeChild.mergeChildren(nextChild);
                return new BranchNode<>(ArrayHelper.assignDelete(this, children, mergeIndex, newMergeChild));
            } else {
                final Tuple2<Node<K, V>, Node<K, V>> distributed = mergeChild.distributeChildren(nextChild);
                return new BranchNode<>(ArrayHelper.assignTwo(children, mergeIndex, distributed.getFirst(), distributed.getSecond()));
            }
        }
    }

    @Nonnull
    @Override
    public Node<K, V> mergeChildren(@Nonnull Node<K, V> sibling)
    {
        final BranchNode<K, V> branch = (BranchNode<K, V>)sibling;
        return new BranchNode<>(ArrayHelper.concat(this, children, branch.children));
    }

    @Nonnull
    @Override
    public Tuple2<Node<K, V>, Node<K, V>> distributeChildren(@Nonnull Node<K, V> sibling)
    {
        final BranchNode<K, V> branch = (BranchNode<K, V>)sibling;
        return Tuple2.of(new BranchNode<>(ArrayHelper.subArray(this, children, branch.children, 0, MIN_CHILDREN)),
                         new BranchNode<>(ArrayHelper.subArray(this, children, branch.children, MIN_CHILDREN, childCount + branch.childCount)));
    }

    @Override
    public int depth()
    {
        return 1 + children[0].depth();
    }

    @Nonnull
    @Override
    public Cursor<JImmutableMap.Entry<K, V>> cursor()
    {
        return LazyMultiCursor.cursor(IndexedArray.retained(children));
    }

    @Nonnull
    @Override
    public SplitableIterator<JImmutableMap.Entry<K, V>> iterator()
    {
        return LazyMultiIterator.iterator(IndexedArray.retained(children));
    }

    @Override
    public void checkInvariants(@Nonnull Comparator<K> comparator)
    {
        if (childCount != children.length) {
            throw new IllegalStateException();
        }
        if (childCount > MAX_CHILDREN) {
            throw new IllegalStateException();
        }
        final int depth = children[0].depth();
        for (int i = 0; i < childCount; ++i) {
            final Node<K, V> child = children[i];
            if (child.depth() != depth) {
                throw new IllegalStateException();
            }
            if (i > 0 && comparator.compare(children[i - 1].baseKey(), children[i].baseKey()) >= 0) {
                throw new IllegalStateException();
            }
            child.checkInvariants(comparator);
        }
    }

    @Nonnull
    @Override
    @SuppressWarnings("unchecked")
    public Node<K, V>[] allocate(int size)
    {
        return new Node[size];
    }

    static <K, V> int findChildIndex(@Nonnull Comparator<K> comparator,
                                     @Nonnull K key,
                                     @Nonnull Node<K, V>[] children,
                                     int beforeFirstChildIndex)
    {
        int first = 0;
        int last = children.length - 1;
        while (first <= last) {
            final int middle = (first + last) >>> 1;
            final K value = children[middle].baseKey();
            final int diff = comparator.compare(key, value);
            if (diff < 0) {
                last = middle - 1;
            } else if (diff > 0) {
                first = middle + 1;
            } else {
                return middle;
            }
        }
        return first > 0 ? first - 1 : beforeFirstChildIndex;
    }
}