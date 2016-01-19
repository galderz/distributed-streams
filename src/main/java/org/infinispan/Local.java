package org.infinispan;

import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.data.SimilarWords;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;

public class Local {

   public static void main(String[] args) throws Exception {
      withLocal(SimilarWords::execute);
   }

   private static void withLocal(BiConsumer<EmbeddedCacheManager, EmbeddedCacheManager> task) {
      try {
         EmbeddedCacheManager cm = Local.createLocalCacheManager();
         try {
            task.accept(cm, cm);
         } finally {
            if (cm != null) cm.stop();
         }
      } catch (Exception e) {
         throw new RuntimeException(e);
      }
   }

   private static EmbeddedCacheManager createLocalCacheManager() {
      EmbeddedCacheManager cm = new DefaultCacheManager();
      cm.getCache();
      return cm;
   }

}
