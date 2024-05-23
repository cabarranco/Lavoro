package com.asbresearch.collector.util;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
public class CollectionUtils {

    public static <T> Collection<List<T>> partitionBasedOnSize(Collection<T> inputList, int size) {
        final AtomicInteger counter = new AtomicInteger(0);
        return inputList.stream()
                .collect(Collectors.groupingBy(s -> counter.getAndIncrement() / size))
                .values();
    }

    public static boolean isEmpty(Collection<?> collection) {
        return org.springframework.util.CollectionUtils.isEmpty(collection);
    }

    public static boolean isEmpty(Map<?, ?> map) {
        return org.springframework.util.CollectionUtils.isEmpty(map);
    }

    public static boolean isEmpty(String word) {
        return word == null || word.isEmpty();
    }
}
