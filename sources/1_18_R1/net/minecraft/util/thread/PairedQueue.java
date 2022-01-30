package net.minecraft.util.thread;

import com.google.common.collect.Queues;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;

public interface PairedQueue<T, F> {

    @Nullable
    F pop();

    boolean push(T t0);

    boolean isEmpty();

    int size();

    public static final class a implements PairedQueue<PairedQueue.b, Runnable> {

        private final Queue<Runnable>[] queues;
        private final AtomicInteger size = new AtomicInteger();

        public a(int i) {
            this.queues = new Queue[i];

            for (int j = 0; j < i; ++j) {
                this.queues[j] = Queues.newConcurrentLinkedQueue();
            }

        }

        @Nullable
        @Override
        public Runnable pop() {
            Queue[] aqueue = this.queues;
            int i = aqueue.length;

            for (int j = 0; j < i; ++j) {
                Queue<Runnable> queue = aqueue[j];
                Runnable runnable = (Runnable) queue.poll();

                if (runnable != null) {
                    this.size.decrementAndGet();
                    return runnable;
                }
            }

            return null;
        }

        public boolean push(PairedQueue.b pairedqueue_b) {
            int i = pairedqueue_b.priority;

            if (i < this.queues.length && i >= 0) {
                this.queues[i].add(pairedqueue_b);
                this.size.incrementAndGet();
                return true;
            } else {
                throw new IndexOutOfBoundsException("Priority %d not supported. Expected range [0-%d]".formatted(new Object[]{i, this.queues.length - 1}));
            }
        }

        @Override
        public boolean isEmpty() {
            return this.size.get() == 0;
        }

        @Override
        public int size() {
            return this.size.get();
        }
    }

    public static final class b implements Runnable {

        final int priority;
        private final Runnable task;

        public b(int i, Runnable runnable) {
            this.priority = i;
            this.task = runnable;
        }

        public void run() {
            this.task.run();
        }

        public int getPriority() {
            return this.priority;
        }
    }

    public static final class c<T> implements PairedQueue<T, T> {

        private final Queue<T> queue;

        public c(Queue<T> queue) {
            this.queue = queue;
        }

        @Nullable
        @Override
        public T pop() {
            return this.queue.poll();
        }

        @Override
        public boolean push(T t0) {
            return this.queue.add(t0);
        }

        @Override
        public boolean isEmpty() {
            return this.queue.isEmpty();
        }

        @Override
        public int size() {
            return this.queue.size();
        }
    }
}
