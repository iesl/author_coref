#!/bin/sh

inputDir=$1
outputDir=$2

mkdir -p $outputDir

mallet import-dir --input $inputDir --output $outputDir/training.mallet --keep-sequence --remove-stopwords
mallet train-topics  --input $outputDir/training.mallet  --num-topics 50 --alpha 50   --output-topic-keys $outputDir/topic-keys.txt --output-doc-topics $outputDir/doc-topics.txt
./scripts/process/format_mallet_output.sh "$outputDir/doc-topics.txt" "$outputDir/topics.db"