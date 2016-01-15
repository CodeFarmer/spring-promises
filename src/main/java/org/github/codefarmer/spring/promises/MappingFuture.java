package org.github.codefarmer.spring.promises;

import org.springframework.util.concurrent.ListenableFuture;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Created by joelgluth on 19/01/2015, as a form of procrastination instead of attacking Netty.
 */
public interface MappingFuture<A>
  extends ListenableFuture<A>
{

  public <B>MappingFuture<B> map(Function<? super A, B> f);
  public <B>MappingFuture<B> flatMap(Function<? super A, ListenableFuture<B>> f);

  public <B>MappingFuture<Tuple2<A, B>> join(ListenableFuture<B> b);
  public <B, C>MappingFuture<C> joinWith(ListenableFuture<B> b, BiFunction<A, B, C> f);

  public MappingFuture<A> rescue(Function<Throwable, A> f);

}
