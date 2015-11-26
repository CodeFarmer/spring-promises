package org.github.codefarmer.spring.promises;

import org.junit.Test;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Created by joelgluth on 19/01/2015.
 */

public class PromiseTest {

  @Test
  public void mapAffectsGet() throws ExecutionException, InterruptedException {

    Promise<Integer> pi = new Promise<>();
    Promise<String> ps = pi.map(Object::toString);

    pi.set(4);
    assertTrue(ps.get().equals("4"));
  }

  @Test
  public void mapAffectsListener() throws ExecutionException, InterruptedException {

    Promise<Integer> pi = new Promise<>();
    Promise<String> ps = pi.map(Object::toString);

    ps.addCallback(new ListenableFutureCallback<String>() {
      @Override
      public void onFailure(Throwable throwable) {
        fail("Integer Promise mapped to String should not fail");
      }

      @Override
      public void onSuccess(String s) {
        assertTrue("4".equals(s));
      }
    });

    pi.set(4);
  }

  @Test
  public void mapThrowingAffectsListener() throws ExecutionException, InterruptedException {

    Promise<Integer> pi = new Promise<>();
    Promise<String> ps = pi.map(integer -> {
      throw new RuntimeException("Something went wrong here");
    });

    pi.set(4);
    try {
      ps.get();
      fail("Get on a Promise that has had its map() throw an exception should fail");
    } catch (Exception e) {
    }
  }

  /**
   * This test exists to make sure that even though an AsyncResult (effectively a constant Future) has listeners added
   * after its result is set on construction, they still get fired when added.
   */
  @Test
  public void asyncResultFiresListeners() {

    final AtomicBoolean shouldBeSet = new AtomicBoolean(false);

    AsyncResult<String> result = new AsyncResult<>("hey!");
    result.addCallback(new ListenableFutureCallback<String>() {
      @Override
      public void onFailure(Throwable throwable) {
        fail("This should never happen");
      }

      @Override
      public void onSuccess(String s) {
        shouldBeSet.set(true);
      }
    });

    assertTrue(shouldBeSet.get());
  }

  @Test
  public void flatMapAffectsGet() throws ExecutionException, InterruptedException {

    Promise<Integer> pi = new Promise<>();
    Promise<String> ps = pi.flatMap((Integer x) -> new AsyncResult<>(x.toString()));

    pi.set(4);
    assertTrue("4".equals(ps.get()));
  }

  @Test
  public void joinWorksInSimpleCase() throws ExecutionException, InterruptedException {

    Promise<Integer> pi = new Promise<>();
    ListenableFuture<String> ps = new AsyncResult<>("Hey");

    Promise<Tuple2<Integer, String>> combined = pi.join(ps);
    pi.set(5);

    assertEquals(combined.get(), new Tuple2<>(5, "Hey"));
    assertEquals(combined.get()._1(), Integer.valueOf(5));
    assertEquals(combined.get()._2(), "Hey");
  }

  @Test
  public void setExceptionWithoutRescueThrows() throws ExecutionException, InterruptedException {

    Promise<Boolean> pb = new Promise<>();
    pb.setException(new IllegalStateException("Something broke"));

    try {
      pb.get();
      fail("get() Should have thrown an exception");
    } catch (ExecutionException ie) {
      if (!(ie.getCause() instanceof IllegalStateException)) {
        fail("get() should have thrown ExecutionException wrapping the IllegalStateException from setException()");
      }
    }
  }

  @Test
  public void rescueSuccessfullyReturnsValue() throws ExecutionException, InterruptedException {

    Promise<String> ps = new Promise<>();
    Promise<String> rescued = ps.rescue(t -> "Ahoy me hearties");

    ps.setException(new NullPointerException());
    assertEquals("Ahoy me hearties", rescued.get());
  }

  @Test
  public void rescueSuccessfullyReturnsValueOnSuccess()
      throws ExecutionException, InterruptedException, TimeoutException {

    Promise<String> ps = new Promise<>();
    Promise<String> rescued = ps.rescue(t -> "Ahoy me hearties");

    ps.set("Foo");
    assertTrue(rescued.isDone());
    assertEquals("Foo", rescued.get());
  }

  static Integer throwNullPointer(String s) {
    throw new NullPointerException();
  }

  static MappableListenableFuture<Integer> throwNullPointerFlat(String s) {
    throw new NullPointerException();
  }

  @Test
  public void rescueofMappedSuccessfullyReturnsValue() throws ExecutionException, InterruptedException {

    Promise<String> ps = new Promise<>();
    Promise<Integer> pi = ps.map(PromiseTest::throwNullPointer);

    Promise<Integer> rescued = pi.rescue(t -> 5);

    ps.setException(new NullPointerException());
    assertEquals(new Integer(5), rescued.get());
  }

  @Test
  public void promiseWorksAsListenableFutureCallback()
      throws ExecutionException, InterruptedException {

    ListenableFuture<String> lfs = new AsyncResult<>("Neep");
    Promise<String> ps = new Promise<>();

    lfs.addCallback(ps);

    assertEquals(ps.get(), "Neep");
  }

  @Test
  public void promisesChain()
      throws ExecutionException, InterruptedException {

    Promise<String> ps0 = new Promise<>();
    Promise<String> ps1 = new Promise<>(ps0);

    ps0.set("Neep");
    assertEquals(ps1.get(), "Neep");
  }

  @Test
  public void mapAfterSetStillWorks() throws ExecutionException, InterruptedException {

    Promise<Integer> pi = new Promise<>();
    pi.set(6);

    MappableListenableFuture<String> ps = pi.map(Object::toString);
    assertEquals(ps.get(), "6");
  }

  @Test
  public void mapAfterSetExceptionStillWorks() throws ExecutionException, InterruptedException {

    Promise<Integer> pi = new Promise<>();
    pi.setException(new RuntimeException("phooey"));

    MappableListenableFuture<String> ps = pi.map(Object::toString);
  }

  @Test
  public void mapAfterExceptionAlreadyThrownWorks() throws ExecutionException, InterruptedException {

    Promise<String> ps = new Promise<>();
    ps.set("4");

    MappableListenableFuture<Integer> pi = ps.map(PromiseTest::throwNullPointer);

    try {
      pi.get();
      fail("get() should result in the NPE getting thrown in the mapping function");
    } catch (ExecutionException ee) {
      assertTrue("Thrown exception in map() should wrap the already-thrown exception that caused it to fire",
          ee.getCause() instanceof NullPointerException);
    }
  }

  @Test
  public void flatMapAfterExceptionAlreadyThrownWorks() throws ExecutionException, InterruptedException {

    Promise<String> ps = new Promise<String>();
    ps.setException(new NullPointerException("foo"));

    MappableListenableFuture<Integer> pi = ps.flatMap(s -> {
      Promise<Integer> p = new Promise<>();
      p.onSuccess(Integer.valueOf(s));
      return p;
    });

    try {
      pi.get();
      fail("get() should result in the NPE getting thrown before the mapping function");
    } catch (ExecutionException ee) {
      assertTrue("Thrown exception in flatMap() should wrap the already-thrown exception that caused it to fire",
          ee.getCause() instanceof NullPointerException);
    }
  }

  @Test
  public void rescueAfterExceptionAlreadyThrownWorks() throws ExecutionException, InterruptedException {

    Promise<String> ps = new Promise<String>();
    ps.setException(new NullPointerException("Hey!"));

    MappableListenableFuture<String> ps2 = ps.rescue(new Function<Throwable, String>() {
      @Override
      public String apply(Throwable throwable) {
        throw new RuntimeException(throwable);
      }
    });

    try {
      ps2.get();
      fail("get() should result in the NPE getting thrown in the rescuing function");
    } catch (ExecutionException ee) {
      assertEquals("Thrown exception in rescue() should be wrapped and thrown by the ExecutionException",
          RuntimeException.class, ee.getCause().getClass());
    }
  }

  @Test
  public void runtimeExceptionDuringRescueGetsPropagated() throws InterruptedException {

    Promise<String> ps1 = new Promise<>();

    MappableListenableFuture<String> ps1_1 = ps1.rescue(t -> {
      throw new RuntimeException(t);
    });

    ps1.setException(new NullPointerException("Hey!"));

    try {
      ps1_1.get();
      fail("get() should result in the NPE getting thrown in the rescuing function");
    } catch (ExecutionException ee) {
      assertEquals("Thrown exception in rescue() should be wrapped and thrown by the ExecutionException",
          RuntimeException.class, ee.getCause().getClass());
    }
  }
}
