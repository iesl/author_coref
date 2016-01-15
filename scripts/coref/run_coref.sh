#!/bin/bash


jarpath="target/author_coref-1.1-SNAPSHOT-jar-with-dependencies.jar"


settingsConf=${1:-config/coref/ParallelCoref.config}
weightsConf=${2:-config/coref/DefaultWeightsWithoutTopicsAndKeywords.config}

time java -Xmx30G -cp $jarpath edu.umass.cs.iesl.author_coref.coreference.RunParallelCoreference \
$(cat $settingsConf) \
$(cat $weightsConf)