RemoteCache<Integer, String> remoteCache = ...
Map<String, Long> results =
      remoteCache.execute("word-count.js", Collections.emptyMap());

