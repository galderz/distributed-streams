package org.infinispan.data;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.Buffer;
import java.nio.CharBuffer;
import java.util.Map;

import static java.util.Collections.emptyMap;

public class RemoteWordCount {

   private static final String SCRIPT_CACHE = "___script_cache";

   public static void main(String[] args) throws Exception {

      //wrong type since BasicLogger has been relocated in embedded package.
//      org.jboss.logging.BasicLogger log = org.infinispan.commons.logging.BasicLogFactory.getLog(RemoteWordCount.class);

      RemoteCacheManager remoteCacheManager = new RemoteCacheManager();
      RemoteCache<Integer, String> cache = remoteCacheManager.getCache();

      // Load text data into the remote cache
      //loadData(cache, "/macbeth.txt");
      cache.put(1, "word1 word2 word3");
      cache.put(2, "word1 word2");
      cache.put(3, "word1");

      // Load the script to execute
      RemoteCache<String, String> scriptCache = remoteCacheManager.getCache(SCRIPT_CACHE);
      addScripts(scriptCache, "/wordCountStream.js");

      Map<String, Long> results = cache.execute("wordCountStream.js", emptyMap());

      System.out.printf("Results are: %s%n", results);
   }

   private static void loadData(RemoteCache<String, String> cache, String fileName) {
      try {
         try (BufferedReader bufferedReader = new BufferedReader(
               new InputStreamReader(RemoteWordCount.class.getResourceAsStream(fileName)))) {
            int chunkSize = 10;
            int chunkId = 0;

            CharBuffer cbuf = CharBuffer.allocate(1024 * chunkSize);
            while (bufferedReader.read(cbuf) >= 0) {
               Buffer buffer = cbuf.flip();
               String textChunk = buffer.toString();
               cache.put(fileName + (chunkId++), textChunk);
               cbuf.clear();
            }
         }
      } catch (IOException e) {
         throw new AssertionError(e);
      }
   }

   public static String loadFileAsString(InputStream is) {
      try {
         StringBuilder sb = new StringBuilder();
         BufferedReader r = new BufferedReader(new InputStreamReader(is));
         for (String line = r.readLine(); line != null; line = r.readLine()) {
            sb.append(line);
            sb.append("\n");
         }
         return sb.toString();
      } catch (IOException e) {
         throw new AssertionError(e);
      }
   }

   private static void addScripts(RemoteCache<String, String> cache, String... scripts) {
      try {
         for (String script : scripts) {
            try (InputStream in = RemoteWordCount.class.getClassLoader().getResourceAsStream(script)) {
               cache.put(script, loadFileAsString(in));
            }
         }
      } catch (IOException e) {
         throw new AssertionError(e);
      }
   }

}
