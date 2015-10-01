#!/bin/sh

jarpath="target/author_coref-1.0-SNAPSHOT-jar-with-dependencies.jar"

java -Xmx5G -cp $jarpath edu.umass.cs.iesl.author_coref.experiment.RunRexaDeterministicBaseline \
$(cat config/coref/RexaDeterministicCoref.config) \
$(cat config/coref/DefaultWeights.config)