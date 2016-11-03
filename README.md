# spring-promises

Some additions to the Spring async Future primitives to allow common operations like map(), flatMap(), and join() (aka 'zip' in Play.F).

Very similar to parts of Play (for Java)'s F library, allowing Promises to wrap
other, less tractable Futures, but designed to be used with Java 8 lambdas and
method references.

## NOTE with recent versions of Spring, this library is OBSOLETE and no longer necessary for most people.

Spring now supports Java 8's CompletableFuture as a return type for various things (including @ResponseBody annotated controller methods). Since CompletableFuture has exactly the sematics of the Promise object (although under different names), the usefulness of this library is now limited to people who either can't use Java 8, or are stuck on Spring 3.6 or so (I haven't gone back to see exactly when the change occurred).

## Maven

    <dependency>
        <groupId>com.github.codefarmer</groupId>
        <artifactId>spring-promises</artifactId>
        <version>1.1</version>
    </dependency>

## Quick start

```java

    ListenableFuture<String>  fs = new AsyncResult<>("4");
    ListenableFuture<Integer> intValuePlusThree = new Promise<>(fs)
        .map(Integer::valueOf)
        .map(i -> i + 3);

    Promise<Integer> five = new Promise<>();
    ListenableFuture<Integer> sum = five
        .join(intValuePlusThree)
        .map(t -> t._1() + t._2());

    sum.addCallback(new ListenableFutureCallback<Integer>() {
      @Override
      public void onFailure(Throwable throwable) {
      }

      @Override
      public void onSuccess(Integer i) {
        System.out.println(i);
      }
    });

    five.set(5);

```

