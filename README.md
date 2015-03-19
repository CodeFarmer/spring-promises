# spring-promises

Some additions to the Spring async Future primitives to allow common operations like map(), flatMap(), and join() (aka 'zip' in Play.F).

Very similar to parts of Play (for Java)'s F library, allowing Promises to wrap
other, less tractable Futures, but designed to be used with Java 8 lambdas and
method references.

## Maven

    <dependency>
        <groupId>com.github.codefarmer</groupId>
        <artifactId>spring-promises</artifactId>
        <version>1.0.3</version>
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

