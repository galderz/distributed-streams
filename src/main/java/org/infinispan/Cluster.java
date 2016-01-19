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

public class Cluster {

   public static void main(String[] args) throws Exception {
      withCluster(SimilarWords::execute);
   }

   private static void withCluster(BiConsumer<EmbeddedCacheManager, EmbeddedCacheManager> task) {
      ExecutorService exec = Executors.newCachedThreadPool();
      Future<EmbeddedCacheManager> f1 = exec.submit(() -> Cluster.createClusteredCacheManager("data.1"));
      Future<EmbeddedCacheManager> f2 = exec.submit(() -> Cluster.createClusteredCacheManager("data.2"));
      try {
         EmbeddedCacheManager cm1 = f1.get();
         EmbeddedCacheManager cm2 = f2.get();
         try {
            System.out.println(cm1.getMembers());
            System.out.println(cm2.getMembers());
            task.accept(cm1, cm2);
         } finally {
            if (cm1 != null) cm1.stop();
            if (cm2 != null) cm2.stop();
            exec.shutdown();
         }
      } catch (Exception e) {
         throw new RuntimeException(e);
      }
   }

   public static EmbeddedCacheManager createClusteredCacheManager(String domain) {
      GlobalConfigurationBuilder global = GlobalConfigurationBuilder.defaultClusteredBuilder();
      global.transport().addProperty("configurationFile", "jgroups-tcp-fastjoin.xml");
      global.globalJmxStatistics().jmxDomain(domain);
      ConfigurationBuilder builder = new ConfigurationBuilder();
      builder.clustering()
            .cacheMode(CacheMode.DIST_SYNC)
            .hash().numOwners(1);
      EmbeddedCacheManager cm = new DefaultCacheManager(global.build(), builder.build());
      cm.getCache();
      return cm;
   }

}
