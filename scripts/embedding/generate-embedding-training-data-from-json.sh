#!/bin/sh

jarpath="target/author_coref-1.0-SNAPSHOT-jar-with-dependencies.jar"

time java -Xmx20G -cp $jarpath edu.umass.cs.iesl.author_coref.process.GenerateWordEmbeddingTrainingDataFromJSON \
--config=config/embedding/GenerateWordEmbeddingTrainingDataFromJSON.config