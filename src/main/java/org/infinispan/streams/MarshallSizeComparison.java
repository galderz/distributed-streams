package org.infinispan.streams;

import org.infinispan.Cache;
import org.infinispan.commons.marshall.AbstractDelegatingMarshaller;
import org.infinispan.commons.marshall.AdvancedExternalizer;
import org.infinispan.commons.marshall.Externalizer;
import org.infinispan.commons.marshall.Marshaller;
import org.infinispan.commons.marshall.SerializeFunctionWith;
import org.infinispan.commons.marshall.StreamingMarshaller;
import org.infinispan.commons.util.Util;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.factories.ComponentRegistry;
import org.infinispan.factories.KnownComponentNames;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;

import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Set;
import java.util.function.Predicate;

public class MarshallSizeComparison {

   static EmbeddedCacheManager CM = createClusteredCacheManager("compare.1");
   static Marshaller marshaller = extractCacheMarshaller(CM.getCache());

   public static void main(String[] args) throws Exception {
      try {
         Predicate<Integer> pser = (Serializable & Predicate<Integer>) i -> i > 70;
         assertMarshalling("Serializable Predicate", pser);

         Predicate<Integer> pserwith = new PredicateSerializeWith();
         assertMarshalling("Predicate @SerializeFunctionWith", pserwith);

         Predicate<Integer> padv = new PredicateInteger();
         assertMarshalling("Advanced predicate", padv);
      } finally {
         CM.stop();
      }
   }

   @SerializeFunctionWith(PredicateSerializeWith.Externalizer0.class)
   private static final class PredicateSerializeWith
         implements Predicate<Integer> {

      public boolean test(Integer i) { return i > 70; }

      public static final class Externalizer0 implements Externalizer<Object> {
         public void writeObject(ObjectOutput oo, Object o) {}
         public Object readObject(ObjectInput input) {
            return new PredicateSerializeWith();
         }
      }
   }

   static class PredicateInteger implements Predicate<Integer> {
      public boolean test(Integer i) { return i > 70; }
   }

   public static final class PredicateIntegerExt implements AdvancedExternalizer<Object> {
      public void writeObject(ObjectOutput oo, Object o) {}
      public Object readObject(ObjectInput input) {
         return new PredicateInteger();
      }

      public Set<Class<?>> getTypeClasses() {
         return Util.<Class<? extends Object>>asSet(PredicateInteger.class);
      }

      public Integer getId() { return 1234; }
   }

   static void assertMarshalling(String name, Object pm) {
      assertMarshalling(name,
            m -> m.objectToByteBuffer(pm),
            (bytes, m) -> {
               Object o = m.objectFromByteBuffer(bytes);
               Predicate<Integer> pu = (Predicate<Integer>) o;
               assert !pu.test(70);
               assert pu.test(71);
            }
      );
   }

   private static AbstractDelegatingMarshaller extractCacheMarshaller(Cache cache) {
      ComponentRegistry cr = extractField(cache, "componentRegistry");
      StreamingMarshaller marshaller = cr.getComponent(StreamingMarshaller.class, KnownComponentNames.CACHE_MARSHALLER);
      return (AbstractDelegatingMarshaller) marshaller;
   }

   private static <T> T extractField(Object target, String fieldName) {
      return (T) extractField(target.getClass(), target, fieldName);
   }

   private static Object extractField(Class type, Object target, String fieldName) {
      while (true) {
         Field field;
         try {
            field = type.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(target);
         }
         catch (Exception e) {
            if (type.equals(Object.class)) {
               throw new RuntimeException(e);
            } else {
               // try with superclass!!
               type = type.getSuperclass();
            }
         }
      }
   }

   static void assertMarshalling(String name,
         ExFunction<Marshaller, byte[]> mf, ExBiConsumer<byte[], Marshaller> uf) {
      marshallToUnmarshall(name, mf, uf);
   }

   @FunctionalInterface
   interface ExFunction<T, R> {
      R apply(T t) throws Exception;
   }

   @FunctionalInterface
   interface ExBiConsumer<T, U> {
      void accept(T t, U u) throws Exception;
   }

   static private void marshallToUnmarshall(String name,
         ExFunction<Marshaller, byte[]> mf, ExBiConsumer<byte[], Marshaller> uf) {
      try {
         byte[] bytes = mf.apply(marshaller);
         System.out.printf("%s payload is %d bytes %n", name, bytes.length);
         uf.accept(bytes, marshaller);
      } catch (Exception e) {
         throw new AssertionError(e);
      }
   }

   public static EmbeddedCacheManager createClusteredCacheManager(String domain) {
      GlobalConfigurationBuilder global = GlobalConfigurationBuilder.defaultClusteredBuilder();
      global.transport().addProperty("configurationFile", "jgroups-tcp-fastjoin.xml");
      global.globalJmxStatistics().jmxDomain(domain);
      global.serialization().addAdvancedExternalizer(new PredicateIntegerExt());
      ConfigurationBuilder builder = new ConfigurationBuilder();
      builder.clustering()
            .cacheMode(CacheMode.DIST_SYNC)
            .hash().numOwners(1);
      EmbeddedCacheManager cm = new DefaultCacheManager(global.build(), builder.build());
      cm.getCache();
      return cm;
   }

}
