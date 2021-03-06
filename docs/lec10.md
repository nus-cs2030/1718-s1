# Lecture 10: Parallel Streams

## Learning Objectives

After attending this lecture, students should:

- be aware that a program can be broken into subtasks to run parallelly and/or concurrently 
- be aware of the issues caused by running the subtasks parallelly and concurrently.
- be aware of that there exist tradeoffs in the number of subtasks and the processing overhead.
- be familiar with how to process a stream parallelly and correctly.
- be familiar with the Java's fork/join framework. 

## Parallel and Concurrent Programming

So far, the programs that we have written in CS2030 run _sequentially_.  What this means is that at any one time, there is only one instruction of the program running on a processor.  

### What is concurrency?

A single core processor can only execute one instruction at one time -- this means that only one _process_  (or less precisely speaking, one application) can run at any one time.  Yet, when we use the computer, it _feels_ as if we are running multiple processes at the same time.  The operating system, behind the scene, is actually switching between the different processes, to give the user an illusion that they are running at the same time.

We can write a program so that it runs concurrently -- by dividing the computation into subtasks called _threads_.  The operating system, behind the scene, can switch between the different threads, to give the user an illusion that the threads are running at the same time.  Such multi-threads programs are useful in two ways: (i) it allows us, the programmers, to separate the unrelated tasks into threads, and write each thread separately; (ii) it improves the utilization of the processor.  For instance, if I/O is in one thread, and UI rendering is in another, then when the processor is waiting for I/O to complete, it can switch to the rendering thread to make sure that the slow I/O does not affect the responsiveness of UI.

### What is parallelism?

While concurrency gives the illusion of subtasks running at the same time, parallel computing refers to the scenario where multiple subtasks are truly running at the same time -- either we have a processor that is capable of running multiple instructions at the same time, or we have multiple cores / processors and dispatch the instructions to the cores / processors so that they are executed at the same time.

All parallel programs are concurrent, but not all concurrent programs are parallel.

Modern computers have more than one cores / processors[^1].
As such, the line between parallelism and concurrency is blurred.  

[^1]: iPhone X comes with A11 Bionic chip with six cores.  The fastest supercomputer in the world as of this writing, the Sunway TaihuLight (神威 太湖之光), has 40,960 processors, each with 256 cores, giving a total of 10,485,760 cores.  

### Parallel computing

Parallel computing is one of the major topics in computer science.  One can teach a whole module (or a focus area) on this topic alone.  The goal of this lecture is not to cover it in depth, but is to expose students in CS2030 to the concept of parallel computing in relation to the stream abstraction in Java 8.

## Parallel Stream 

We have seen that Java `Stream` class is a powerful and useful class for processing data in declarative style.  But, we have not fully unleash the power of `Stream`.  The neatest thing about `Stream` is that it allows parallel operations on the elements of the stream in one single line of code.  

Let's consider the following program that prints out all the prime numbers between 1 and 999,999.

```Java
    IntStream.range(1, 1_000_000)
        .filter(x -> isPrime(x))
        .forEach(System.out::println);
```

We can parallelize the code by adding the call `parallel()` into the stream.

```Java
    IntStream.range(1, 1_000_000)
        .filter(x -> isPrime(x))
        .parallel()
        .forEach(System.out::println);
```

You may observe that the output has been reordered, although the same set of numbers are still being produced.  This is because `Stream` has broken down the numbers into subsequences, and run `filter` and `forEach` for each subsequences in parallel.  Since there is no coordination among the parallel tasks on the order of the printing, whichever parallel tasks that complete first will output the result to screen first, causing the sequence of numbers to be reordered.

If you want to produce the output in the order of input, use `forEachOrdered` instead of `forEach`, we will loose some benefits of parallelization because of this.

Suppose now that we want to compute the number of primes below 1,000,000.  We can run:

```Java
    IntStream.range(1, 1_000_000)
        .filter(x -> isPrime(x))
        .parallel()
        .count()
```

The code above produce the same output regardless of it is being parallelized or not.  

Note that the task above is stateless and does not produce any side effect.  Furthermore, each element is processed individually without depending on other elements.  Such computation is sometimes known as _embarrassingly parallel_.  The only communication needed for each of the parallel subtask is to combine the result of `count()` from the subtasks into the final count (which has been implemented in `Stream` for us.

### How to parallelize a stream

You have seen that adding `parallel()` to the chain of calls in a stream enables parallel processing of the stream.  Note that `parallel()` is a lazy operation -- it merely marks the stream to be process in parallel.  As such, you can insert the call to `parallel()` anywhere in the chain.

!!! note "sequential()"
    There is a method `sequential()` which marks the stream to be process sequentially.  If you call both `parallel()` and `sequential()` in a stream,
    the last call "wins".  The example below processes the stream 
    sequentially:
    ```
    s.parallel().filter(x -> x < 0).sequential().forEach(..); 
    ```

Another way to create a parallel stream is to call the method `parallelStream()` instead of `stream()` of the `Collector` class.  Doing so would create a stream that will be processed in parallel from the collection.

### What can be parallelized?

To ensure that the output of the parallel execution is correct, the stream operations must not _interfere_ with the stream data, and most of time must be _stateless_.  Side-effects should be kept to minimum.

### Interference
Interference means that one of the stream operation modifies the source of the stream during the execution of the terminal operation.  For instance:

```Java
List<String> list = new ArrayList<>(Arrays.asList("Luke", "Leia", "Han"));
list.stream()
    .peek(name -> {
         if (name.equals("Han")) {
           list.add("Chewie"); // they belong together
         }
      })
    .forEach(i -> {});
```

Would cause `ConcurrentModificationException` to be thrown.  Note that this non-interference rule applies even if we are using `stream()` instead of `parallelStream()`.

### Stateless
A stateful lambda is one where the result depends on any state that might change during the execution of stream.

For instance, the `generate` and `map` operations below are stateful, since they depend on the events in the queue and the states of the shops.  Parallelizing this may lead to incorrect output.  To ensure that the output is correct, additional work needs to be done to ensure that state updates are visible to all parallel subtasks.

```Java
Stream.generate(this.events::poll)
    .takeWhile(event -> event != null)
    .filter(event -> event.happensBefore(sim.expireTime())) 
    .peek(event -> event.log())
    .map(event -> sim.handle(event))
    .forEach(eventStream -> this.schedule(eventStream));
```

### Side Effects
Side-effects can lead to incorrect results in parallel execution.  Consider the following code:

```Java
List<Integer> list = new ArrayList<>(
    Arrays.asList(1,3,5,7,9,11,13,15,17,19));
List<Integer> result = new ArrayList<>();
list.parallelStream()
    .filter(x -> isPrime(x))
    .forEach(x -> result.add(x));
```

The `forEach` lambda generates a side effect -- it modifies `result`.  `ArrayList` is what we call a non-thread-safe data structure.  If two threads manipulate it at the same time, incorrect result may result.

If we use `Collectors.toList()` instead, we can achieve the intended result without visible side effects.

### Associativity
The `reduce` operation is inherently parallelizable, as we can easily reduce each sub-streams and then use the `combiner` to combine the results together.  Recall this example from Lecture 9:

```Java
Stream.of(1,2,3,4).reduce(1, (x,y)->x*y, (x,y)->x*y);
```

There are several rules that the `identity`, the `accumulator` and the `combiner` must follow:

- `combiner.apply(identity, i)` must be equal to `i`.
- The `combiner` and the `accumulator` must be associative -- the order of applying must not matter.
- The `combiner` and the `accumulator` must be compatible -- `combiner.apply(u, accumulator.apply(identity, t))` must equal to `accumulator.apply(u, t)`

The multiplication example above meetings the three rules:
    
- `i * 1` equals `i`
- `(x * y) * z` equals `x * (y * z)`
- `u * (1 * t)` equals `u * t`

## Performance of Parallel Stream

Let's go back to:

```Java
IntStream.range(1, 1_000_000)
    .filter(x -> isPrime(x))
    .parallel()
    .count()
```
 
How much time can we save by parallelizing the code above?

Let's use the [`Instant`](https://docs.oracle.com/javase/9/docs/api/java/time/Instant.html) and [`Duration`](https://docs.oracle.com/javase/9/docs/api/java/time/Duration.html) class from Java to help us:

```Java
    Instant start = Instant.now();
    long howMany = IntStream.range(1,1000000)
        .filter(x -> isPrime(x))
        .parallel()
        .count();
    Instant stop = Instant.now();
    System.out.println(howMany + " " + Duration.between(start,stop).toMillis() + " ms");
```

The code above measures roughly the time it takes to count the number of primes below 1,000,000.  On my iMac, it takes about 300-320 ms.  If I remove `parallel()`, it takes about 500 ms.  So we gain about 36 - 40% performance.

Can we parallelize some more?  Remember how we implement `isPrime`[^2]

```Java
  boolean isPrime(int n) {
    return IntStream.range(2, (int)Math.sqrt(n) + 1)
        .noneMatch(x -> n % x == 0);
  }
```

Let's parallelize this to make this even faster!

```Java
  boolean isPrime(int n) {
    return IntStream.range(2, (int)Math.sqrt(n) + 1)
        .parallel()
        .noneMatch(x -> n % x == 0);
  }
```

[^2]: This is a more efficient version of the code you have seen, since it stops testing after the square root of the $n$.

If you run the code above, however, you will find that the code is not as fast as we expect. On my iMac, it takes about 12.7s, about 25 times slower!

_Parallelizing a stream does not always improve the performance_.

What is going on?  To understand this, we have to delve a bit deeper into how Java implements the parallel streams.  

### Thread Pools and Fork/Join

Internally, Java maintains pool of _worker threads_.  A worker thread is an abstraction for running a task.  We can submit a task to the pool for execution, the task will join queue.  The worker thread can pick a task from the queue to execute.  When it is done, it pick another task, if one exists in the queue, and so on -- not unlike our `Server` (worker thread) and `Customer` (task).

A `ForkJoinPool` is a class the implements a thread pool with a particular semantic --- the task that the worker runs must specify `fork` -- how to create subtasks, and `join` -- how to merge the results from the subtasks.

In the case of a parallel stream, `fork` will create subtasks running the same chain of operations on sub-streams, and when done, run `join` to combine the results (e.g., `combiner` for `reduce` is run in `join`).  `fork` and `join` can be recursive -- for instance, a `fork` operation can split the stream into two subtasks.  The subtasks can further split the sub-streams into four smaller sub-streams, and so on, until the size of the sub-stream is small enough that the task is actually invoked.

To define a task, we subclass from `RecursiveTask<T>` (if the task returns a value of type `T`) or `RecursiveAction` (if the task does not return a value).

Here is an example task that we can submit to ForkJoinPool:

```Java
  static class BinSearch extends RecursiveTask<Boolean> {
    final int FORK_THRESHOLD = 2;
    int low;
    int high;
    int toFind;
    int[] array;

    BinSearch(int low, int high, int toFind, int[] array) {
      this.low = low;
      this.high = high;
      this.toFind = toFind;
      this.array = array;
    }

    @Override
    protected Boolean compute() {
      // stop splitting into subtask if array is already small.
      if (high - low < FORK_THRESHOLD) {
        for (int i = low; i < high; i++) {
          if (array[i] == toFind) {
            return true;
          }
        }
        return false;
      } 

      int middle = (low + high)/2;
      BinSearch left = new BinSearch(low, middle, toFind, array);
      BinSearch right = new BinSearch(middle, high, toFind, array);
      left.fork();
      return right.compute() || left.join();
    }
  }
```

To run the task, we call the `invoke` method of `ForkJoinPool`, which executes the given task immediately and return the result.

```Java
    BinSearch searchTask = new BinSearch(0, array.length, 12, array);
    boolean found = ForkJoinPool.commonPool().invoke(searchTask)
```

The task above recursively search for an element in the left half and right half of the array.  Note that this is similar to, but is NOT binary search.  Binary search of course just search in either the left or the right side, depending on the middle value, and is not parallel.

### ForkJoinPool overhead

In the example above, you can see that creating subtasks incur some overhead (new task objects, copying of parameters into objects, etc).  In the `isPrime` example above, the task is trivial (checking `n % x == 0`), and so, by parallelizing it, we are actually creating more work for Java to do!  It is much more efficient if we simply check for `n % x == 0` sequentially.

The moral of the story is, parallelization is worthwhile if the task is complex enough that the benefit of parallelization outweighs the overhead.  While we discuss this in the context of parallel streams, this principle holds for all parallel and concurrent programs.

### Ordered vs. Unordered Source

Whether or not the stream elements are _ordered_ or _unordered_ also plays a role in the performance of parallel stream operations.  A stream may define an _encounter order_.  Streams created from `iterate`, ordered collections (e.g., `List` or arrays), from `of`, are ordered.  Stream created from `generate` or unordered collections (e.g., `Set`) are unordered.

Some stream operations respect the encounter order.  For instance, both `distinct` and `sorted` preserve the original order of elements (if ordering is preserved, we say that an operation is _stable_).

The parallel version of `findFirst`, `limit`, and `skip` can be expensive on ordered stream.  

If we have an ordered stream and respecting the original order is not important, we can call `unordered()` as part of the chain command to make the parallel operations much more efficient.

The following, for example, takes about 700 ms on my iMac:

```Java
    Stream.iterate(0, i -> i + 7)
        .parallel()
        .limit(10_000_000)
        .filter(i -> i % 64 == 0)
        .forEachOrdered(i -> { });
```

But, with `unordered()` inserted, it takes about 350ms, a 2x speed up!

```Java
    Stream.iterate(0, i -> i + 7)
        .parallel()
        .unordered()
        .limit(10_000_000)
        .filter(i -> i % 64 == 0)
        .forEachOrdered(i -> { });
```

### Collectors

To wrap up, we will revisit the `Collector` class.  Recall that `collect` is a mutable version of `reduce` on `Stream`.  Being mutable, parallelizing it is tricky.  Luckily, we only need to provide hints to the Collector class, which will optimize the implementation for us.  The hint is given as part of the `characteristics()` method (the last method that we did not cover last week).

The `characteristics()` method returns a `Set` that contains a combination of three enums:

- `CONCURRENT` to indicate that the container that the supplier created can support accumulator function being called concurrently from multiple threads,
- `IDENTITY_FINISH` to indicate the the finisher function is the identity function, and can be skipped.
- `UNORDERED` to indicate that the collection operation does not necessary preserve the encounter order of the elements.

The operation `collect` will only be parallelized if the following three conditions hold:

- The stream is parallel
- The collector has characteristic `CONCURRENT` 
- The stream is unordered or has characteristic `UNORDERED`

Of course, if we tell the collector has the characteristic `CONCURRENT`, the container that we use must actually supports that!  Luckily for us, Java `java.util.concurrent` package provides many collections that support concurrency, including `CopyOnWriteArrayList`, `ConcurrentHashMap`, etc.  Obviously these are more expensive.  For instance, `CopyOnWriteArrayList` creates a fresh copy of the underlying array whenever there is a mutative operation (e.g., `add`, `set`, etc), not unlike your `LambdaList`.  

Again, this is the overhead cost of parallelization -- the cost that might not outweigh the benefit of parallelization, and should be considered carefully.
