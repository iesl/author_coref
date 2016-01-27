#!/bin/sh


echo "Training word embeddings"

START_TIME=$(date +%x_%H:%M:%S:%N)
START=$(date +%s)

jarpath="target/author_coref-1.1-SNAPSHOT-jar-with-dependencies.jar"
training_data=${1:-"data/embedding/training_data.txt"}
output_dir=${2:-"data/embedding"}
output_vocabulary="$output_dir/embedding-vocab.txt"
output_embeddings="$output_dir/embeddings.txt"

echo "Training Data File: $training_data"
echo "Output Dir: $output_dir"

mkdir -p $output_dir

num_threads=20
min_count=50
num_iterations=5 #Adjust this depending on the size of your training data

java -Xmx40G -cp ${jarpath} cc.factorie.app.nlp.embeddings.WordVec \
--min-count=$min_count \
--train=$training_data \
--output=$output_embeddings \
--save-vocab=$output_vocabulary \
--encoding="UTF-8" \
--threads=$num_threads \
--cbow=true \
--window=5 \
--negative=7 \
--size=200 \
--sample=1e-5 \
--num-iterations=$num_iterations

END=$(date +%s)
END_TIME=$(date +%x_%H:%M:%S:%N)

RTSECONDS=$(($END - $START))
echo -e "Running Time (seconds) = $RTSECONDS "
echo -e "Started script at $START_TIME"
echo -e "Ended script at $END_TIME"