package org.infinispan.streams;

import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.IntStream;

public class WhatIsStream {

   public static void main(String[] args) throws Exception {
      List<Integer> numbers = Arrays.asList(
            4, 74, 20, 97, 118, 50, 97, 34, 48);
      numbers.stream()
            .filter(i -> i > 70) // Returns Stream<Integer>
            .map(n -> new String(Character.toChars(n))) // Returns Stream<String>
            .reduce("", String::concat); // Returns "Java"

      IntStream iterStream = IntStream.iterate(0, i -> i + 1);

      IntStream.iterate(0, i -> i + 1)
         .limit(10) // Returns IntStream
         .forEach(System.out::println); // Returns void

      IntStream.iterate(0, i -> i + 1)
            .forEach(System.out::println);

      numbers.stream()
            .filter((Serializable & Predicate<Integer>) i -> i > 70)
            .map(n -> new String(Character.toChars(n))) // Returns Stream<String>
            .reduce("", String::concat); // Returns "Java"


   }

   @org.infinispan.commons.marshall.SerializeFunctionWith(value = PredicateExternalizer.Externalizer0.class)
   private static final class PredicateExternalizer<K> implements Predicate<Integer> {
      public boolean test(Integer i) { return i > 70; }

      public static final class Externalizer0
            implements org.infinispan.commons.marshall.Externalizer<Object> {
         public void writeObject(ObjectOutput oo, Object o) {}
         public Object readObject(ObjectInput input) { return new PredicateExternalizer(); }
      }
   }

}
