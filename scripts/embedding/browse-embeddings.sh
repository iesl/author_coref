#!/bin/sh

embeddingfile=${1:-"data/embedding/embeddings.txt"}

jarpath="target/author_coref-1.0-SNAPSHOT-jar-with-dependencies.jar"


java -Xmx20G -cp $jarpath edu.umass.cs.iesl.author_coref.embedding.BrowseEmbeddings --embedding-file=$embeddingfile
