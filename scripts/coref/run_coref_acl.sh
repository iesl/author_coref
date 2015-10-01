#!/bin/sh


jarpath="target/author_coref-1.0-SNAPSHOT-jar-with-dependencies.jar"


settingsConf=${1:-config/coref/ACLCoref.config}
weightsConf=${1:-config/coref/DefaultWeights.config}

time java -Xmx20G -cp $jarpath edu.umass.cs.iesl.author_coref.coreference.RunParallelCoreferenceACL \
$(cat $settingsConf) \
$(cat $weightsConf)