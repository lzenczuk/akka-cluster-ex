This project contains multiple main classes. Because of this one application.conf file can't be use in this project. Configuration file have to be provided using VM option -Dconfig.resource=config_file name.
  
Required configurations:
PubSubMain - pub_sub.conf
ClusterMonitoringMain - cluster_node1.conf, cluster_node2.conf, cluster_node3.conf
ClusterManualJoinMain - cluster_manual/node1.conf, cluster_manual/node2.conf, cluster_manual/node3.conf

In Intellij Idea the same main class may be run multiple times.
  1. Run class
  2. In run menu choose Edit configurations...
  3. Copy selected configuration (from menu bar)
  4. Modify copied configuration pointing for example to different config file
  

  