#!/bin/sh

jarpath="target/author_coref-1.0-SNAPSHOT-jar-with-dependencies.jar"


aclDir=${1:-"data/acl-grobid"}
aclFileCodec=${2:-"iso-8859-1"}
outputDir=${3:-"data/acl-mallet-input"}


time java -Xmx40G -cp $jarpath edu.umass.cs.iesl.author_coref.process.GenerateMalletInputDataFromACL \
--acl-dir=$aclDir \
--acl-file-codec=$aclFileCodec \
--output-dir=$outputDir