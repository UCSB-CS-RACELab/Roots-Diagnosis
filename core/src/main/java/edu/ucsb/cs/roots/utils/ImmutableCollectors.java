package edu.ucsb.cs.roots.utils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

public class ImmutableCollectors {

    public static <T> Collector<T, ImmutableList.Builder<T>, ImmutableList<T>> toList() {
        return Collector.of(ImmutableList.Builder::new, ImmutableList.Builder::add,
                (l, r) -> l.addAll(r.build()), ImmutableList.Builder<T>::build);
    }

    public static <T, K, V> Collector<T, ?, ImmutableMap<K, V>> toMap(
            Function<? super T, ? extends K> keyMapper,
            Function<? super T, ? extends V> valueMapper) {

        Supplier<ImmutableMap.Builder<K, V>> supplier =
                ImmutableMap.Builder::new;

        BiConsumer<ImmutableMap.Builder<K, V>, T> accumulator =
                (b, t) -> b.put(keyMapper.apply(t), valueMapper.apply(t));

        BinaryOperator<ImmutableMap.Builder<K, V>> combiner =
                (l, r) -> l.putAll(r.build());

        Function<ImmutableMap.Builder<K, V>, ImmutableMap<K, V>> finisher =
                ImmutableMap.Builder::build;

        return Collector.of(supplier, accumulator, combiner, finisher);
    }

}
