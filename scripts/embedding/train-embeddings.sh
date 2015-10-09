#!/bin/sh


echo "Training word embeddings"

START_TIME=$(date +%x_%H:%M:%S:%N)
START=$(date +%s)

jarpath="target/author_coref-1.0-SNAPSHOT-jar-with-dependencies.jar"
training_data="data/embedding/training_data.txt"
num_threads=20
output_vocabulary="data/embedding/embedding-vocab.txt"
output_embeddings="data/embedding/embeddings.txt"

java -Xmx40G -cp ${jarpath} cc.factorie.app.nlp.embeddings.WordVec \
--min-count=200 \
--train=$training_data \
--output=$output_embeddings \
--save-vocab=$output_vocabulary \
--encoding="UTF-8" \
--threads=$num_threads

END=$(date +%s)
END_TIME=$(date +%x_%H:%M:%S:%N)

RTSECONDS=$(($END - $START))
echo -e "Running Time (seconds) = $RTSECONDS "
echo -e "Started script at $START_TIME"
echo -e "Ended script at $END_TIME"