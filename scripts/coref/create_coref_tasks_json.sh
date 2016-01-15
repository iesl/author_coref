#!/bin/sh

jarpath="target/author_coref-1.1-SNAPSHOT-jar-with-dependencies.jar"

time java -Xmx20G -cp $jarpath edu.umass.cs.iesl.author_coref.process.GenerateCorefTasksFromJSON \
--config=config/coref/CreateCorefTasksJSON.config
