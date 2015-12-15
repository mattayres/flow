package com.lithium.flow.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.annotation.Nullable;

import com.google.common.collect.Lists;

/**
 * @author pradeep.gollakota
 */
public class Batcher<T> {

	private final Batch<T> POISON_PILL;
	private final int batchSize;
	private final BlockingQueue<Batch<T>> batchQueue;
	private Batch<T> currentBatch;
	private boolean finished;

	public Batcher(int batchSize) {
		this(batchSize, Integer.MAX_VALUE);
	}

	public Batcher(int batchSize, int maxNumBatches) {
		this.batchSize = batchSize;
		this.currentBatch = new Batch<>(batchSize);
		this.batchQueue = new LinkedBlockingQueue<>(maxNumBatches);
		this.finished = false;
		this.POISON_PILL = new Batch<>(0);
	}

	public synchronized void offer(T item) {
		if (finished) {
			throw new IllegalStateException("Cannot offer more elements after finish() has been called");
		}

		currentBatch.add(item);
		if (currentBatch.size() >= batchSize) {
			batchQueue.offer(currentBatch);
			currentBatch = new Batch<>(batchSize);
		}
	}

	@Nullable
	public Batch<T> take() throws InterruptedException {
		Batch<T> toReturn = batchQueue.take();
		if (toReturn == POISON_PILL) {
			batchQueue.offer(POISON_PILL);
			return null;
		}
		return toReturn;
	}

	public synchronized void finish() {
		if (finished) {
			throw new IllegalStateException("Batcher already finished");
		}
		finished = true;
		batchQueue.offer(currentBatch); // Offer any remaining items as a partial batch
		batchQueue.offer(POISON_PILL); // Kill the consumers
		currentBatch = null;
	}

	public static final class Batch<T> implements Iterable<T> {

		private final ArrayList<T> itemQueue;

		private Batch(int size) {
			this.itemQueue = Lists.newArrayListWithCapacity(size);
		}

		private void add(T item) {
			itemQueue.add(item);
		}

		public int size() {
			return itemQueue.size();
		}

		@Override
		public Iterator<T> iterator() {
			return new Iterator<T>() {
				private final Iterator<T> internal = itemQueue.iterator();

				@Override
				public boolean hasNext() {
					return internal.hasNext();
				}

				@Override
				public T next() {
					return internal.next();
				}
			};
		}
	}
}
