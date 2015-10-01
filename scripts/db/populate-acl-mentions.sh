#!/bin/sh

jarpath="target/author_coref-1.0-SNAPSHOT-jar-with-dependencies.jar"

time java -Xmx40G -cp $jarpath edu.umass.cs.iesl.author_coref.db.PopulateAuthorMentionDBWithACLMentions \
--config=config/db/PopulateACLMentions.config
