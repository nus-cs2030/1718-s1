Some common mistakes:

1. Forget to add `join()`

    That some solutions get inconsistent results for large matrix size and run extremely fast is because they forgot to join forked tasks.

2. Race condition

    Another group of students got inconsistent results because they manipulate the same element of the result matrix in different threads.

3. OutOfMemory/StackOverflowError

    Some solutions create quite a number of `RecursiveTask/Action` which result in `OutOfMemoryException` and `StackOverflowError`.
