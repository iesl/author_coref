#!/bin/sh

jarpath="target/author_coref-1.0-SNAPSHOT-jar-with-dependencies.jar"

java -Xmx5G -cp $jarpath edu.umass.cs.iesl.author_coref.experiment.RunRexa \
$(cat config/coref/RexaCoref.config) \
$(cat config/coref/DefaultWeights.config)