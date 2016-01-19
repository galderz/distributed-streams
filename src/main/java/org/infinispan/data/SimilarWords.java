package org.infinispan.data;

import com.sun.org.apache.bcel.internal.classfile.SourceFile;
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
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
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
