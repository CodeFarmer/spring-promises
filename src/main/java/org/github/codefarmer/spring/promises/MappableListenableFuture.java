package org.github.codefarmer.spring.promises;

import org.springframework.util.concurrent.ListenableFuture;

import java.util.function.Function;

/**
 * Created by joelgluth on 19/01/2015, as a form of procrastination instead of attacking Netty.
 */
public interface MappableListenableFuture<A>
  extends ListenableFuture<A>
{

  public <B>MappableListenableFuture<B> map(Function<? super A, B> f);
  public <B>MappableListenableFuture<B> flatMap(Function<? super A, ListenableFuture<B>> f);

  public <B>MappableListenableFuture<Tuple2<A, B>> join(ListenableFuture<B> b);

  public MappableListenableFuture<A> rescue(Function<Throwable, A> f);

}
