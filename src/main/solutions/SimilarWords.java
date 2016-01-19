package org.infinispan.data;

import org.infinispan.Cache;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.stream.CacheCollectors;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class SimilarWords {

   final static Random R = new Random();

   public static void execute(EmbeddedCacheManager cm1, EmbeddedCacheManager cm2) {
      Cache<Integer, String> cache = cm1.getCache();

      // Get the list of words
      List<String> words = getWordList();

      // Generate random sentences of 4 words
      List<String> phrases = IntStream.rangeClosed(0, 4000)
            .map(x -> R.nextInt(words.size() - 4))
            .mapToObj(i -> String.join(" ", words.subList(i, i + 4)))
            .collect(Collectors.toList());

      // Store sentences along with index in cache
      IntStream.range(0, phrases.size())
            .forEach(i -> cache.put(i, phrases.get(i)));

      // Calculate a histogram showing the distribution of word lengths in the phrases
      Map<Integer, Long> collect = cache.values().stream()
         //.flatMap(p -> Arrays.asList(p.split(" ")).stream())
         .flatMap((Serializable & Function<String, Stream<String>>) p -> Arrays.asList(p.split(" ")).stream())
         //.map(w -> new int[]{w.length(), 1})
         .map((Serializable & Function<String, int[]>) w -> new int[]{w.length(), 1})
         //.collect(Collectors.groupingBy(h -> h[0], Collectors.counting()));
         .collect(CacheCollectors.serializableCollector(() -> Collectors.groupingBy(h -> h[0], Collectors.counting())));

      collect.entrySet().stream().forEach(e ->
            System.out.printf("%d chars words: %d occurrences%n", e.getKey(), e.getValue()));

      // Empowered by the Levenshtein distance implementation, given a word
      // find similar words in the cache  according to the provided maximum
      // edit distance:
      System.out.printf("Words similar to `cat` with 1 character difference: %s%n",
            findDistance("cat", 1, cache));
   }

   static List<String> findDistance(String word, int maxEditDistance, Cache<Integer, String> cache) {
      return cache.values().stream()
            //.flatMap(p -> Arrays.asList(p.split(" ")).stream())
            .flatMap((Serializable & Function<String, Stream<String>>) p -> Arrays.asList(p.split(" ")).stream())
            //.filter(w -> LevenshteinDistance.computeLevenshteinDistance(w, word) <= maxEditDistance)
            .filter((Serializable & Predicate<String>) w -> LevenshteinDistance.computeLevenshteinDistance(w, word) <= maxEditDistance)
            //.collect(Collectors.toList());
            .collect(CacheCollectors.serializableCollector(() -> Collectors.toList()));
   }

   static List<String> getWordList() {
      List<String> l = new ArrayList<>();
      Path file = FileSystems.getDefault().getPath("/usr/share/dict/words");
      try (InputStream in = Files.newInputStream(file);
           BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
         String line = null;
         while ((line = reader.readLine()) != null) l.add(line);
         return l;
      } catch (IOException x) {
         throw new AssertionError(x);
      }
   }

}
