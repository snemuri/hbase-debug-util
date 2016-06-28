# hbase-debug-util

java -cp \`hbase classpath\` com.hbase.util.Status [--zookpeer-server \<comma seperated list\>] [--zookeper-znode \<znode\>] [--zookeeper-port \<zk port\>] [--table-name \<all|tablename\>] --cmd \<compaction|onlineregion|offlineregion\>  2>/tmp/_progress.log
