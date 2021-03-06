package org.github.codefarmer.spring.promises;

import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.util.concurrent.SettableListenableFuture;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Created by joelgluth on 19/01/2015.
 *
 * This is not super far from being a reimplementation of parts of java.util.concurrent's
 * CompletableFuture class in Spring terms (and with less silly names).
 *
 * Actually what everybody wants is Scala futures, but importing the entire Scala library feels a
 * bit poor, and besides this was interesting.
 */
public class Promise<A>
  extends SettableListenableFuture<A>
  implements MappingFuture<A>, ListenableFutureCallback<A>
{

  static class JoinedPromise<A, B>
  extends Promise<Tuple2<A, B>>
  {

    // This may be overkill. But hey, I have references!
    final AtomicReference<A> a = new AtomicReference<>();
    final AtomicReference<B> b = new AtomicReference<>();

    CountDownLatch cdl = new CountDownLatch(2);

    <X>void addLatchedListener(AtomicReference<X> ref, ListenableFuture<X> lfx)
    {
      lfx.addCallback(new ListenableFutureCallback<X>() {

        @Override
        public void onFailure(Throwable throwable) {
          setException(throwable);
        }

        @Override
        public void onSuccess(X aresult) {
          ref.set(aresult);
          cdl.countDown();
          if (cdl.getCount() == 0) {
            set(new Tuple2<>(a.get(), b.get()));
          }
        }
      });

    }

    public JoinedPromise(ListenableFuture<A> lfa, ListenableFuture<B> lfb)
    {
      addLatchedListener(a, lfa);
      addLatchedListener(b, lfb);
    }

  }

  public Promise() {
    super();
  }

  public Promise(ListenableFuture<A> other) {
    super();
    other.addCallback(this);
  }

  @Override
  public <B> Promise<B> map(Function<? super A, B> f) {

    final Promise<B> mappedPromise = new Promise<>();

    try {

      addCallback(new ListenableFutureCallback<A>() {
        @Override
        public void onFailure(Throwable throwable) {
          mappedPromise.setException(throwable);
        }

        @Override
        public void onSuccess(A a) {
          // Awesomely, if f throws an exception, the calling-back code will pass it to onFailure :)
          mappedPromise.set(f.apply(a));
        }
      });

    }
    catch (Exception e) { // this can happen due to the mechanics of Spring's CallbackListenerRegistry
      mappedPromise.setException(e);
    }

    return mappedPromise;

  }

  @Override
  public <B> Promise<B> flatMap(Function<? super A, ListenableFuture<B>> f) {

    final Promise<B> flatMappedPromise = new Promise<>();

    try {

      addCallback(new ListenableFutureCallback<A>() {
        @Override
        public void onFailure(Throwable throwable) {
          flatMappedPromise.setException(throwable);
        }

        @Override
        public void onSuccess(A a) {

          f.apply(a).addCallback(new ListenableFutureCallback<B>() {
            @Override
            public void onFailure(Throwable throwable) {
              flatMappedPromise.setException(throwable);
            }

            @Override
            public void onSuccess(B b) {
              flatMappedPromise.set(b);
            }
          });
        }
      });

    }
    catch (Exception e) { // this can happen due to the mechanics of Spring's CallbackListenerRegistry
      flatMappedPromise.setException(e);
    }

    return flatMappedPromise;

  }

  @Override
  public <B> Promise<Tuple2<A, B>> join(ListenableFuture<B> bf) {
    return new JoinedPromise<>(this, bf);
  }

  @Override
  // I actually feel like join() should be implemented in terms of joinWith(), but it was written first :P
  public <B, C> MappingFuture<C> joinWith(ListenableFuture<B> b, BiFunction<A, B, C> f) {
    return join(b).map(t -> f.apply(t._1(), t._2()));
  }

  @Override
  public Promise<A> rescue(Function<Throwable, A> f) {

    final Promise<A> rescued = new Promise<>();

    try {

      addCallback(new ListenableFutureCallback<A>() {
        @Override
        public void onFailure(Throwable throwable) {
          try {
            rescued.set(f.apply(throwable));
          }
          catch (Exception e) {
            rescued.setException(e);
          }
        }

        @Override
        public void onSuccess(A a) {
          rescued.set(a);
        }
      });

    }
    catch (Exception e) { // this can happen due to the mechanics of Spring's CallbackListenerRegistry
      rescued.setException(e);
    }

    return rescued;

  }

  // ListenableFutureCallback methods

  @Override
  public void onFailure(Throwable throwable) {
    setException(throwable);
  }

  @Override
  public void onSuccess(A o) {
    set(o);
  }

}
