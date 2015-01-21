package org.github.codefarmer.spring.promises;

import org.junit.Test;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

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
    }
    catch (Exception e) {
    }

  }
  /**
   * This test exists to make sure that even though an AsyncResult (effectively a constant Future)
   * has listeners added after its result is set on construction, they still get fired when added.
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
    }
    catch (ExecutionException ie) {
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
  public void promiseWorksAsListenableFutureCallback()
      throws ExecutionException, InterruptedException {

    ListenableFuture<String> lfs = new AsyncResult<>("Neep");
    Promise<String> ps = new Promise<>();

    lfs.addCallback(ps);

    assertEquals(ps.get(), "Neep");

  }

  @Test
  public void PromisesChain()
      throws ExecutionException, InterruptedException {

    Promise<String> ps0 = new Promise<>();
    Promise<String> ps1 = new Promise<>(ps0);

    ps0.set("Neep");
    assertEquals(ps1.get(), "Neep");

  }


}
