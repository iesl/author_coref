#!/bin/sh


jarpath="target/author_coref-1.0-SNAPSHOT-jar-with-dependencies.jar"


java -Xmx30G -cp $jarpath edu.umass.cs.iesl.author_coref.utilities.GenerateKeywordsACL \
--config=config/utilities/GenerateKeywordsACL.config