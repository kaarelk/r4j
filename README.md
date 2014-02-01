r4j
===

Raft implementation in java.
Supported features:
  * Leader election
  * Log replication
  * Safety
  
TODO:
  * Batching
  * Log compaction
  * Cluster reconfiguration (resize)
  

restful-r4j
===


Example implementation using r4j library.
Restful data store using:
  * Jetty + jersey
  * Zeromq
  * Jackson
  * File based log
  
TODO:
  * leveldb data store (instead of in-memory hash map)
  * log compaction

