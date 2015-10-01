#!/bin/sh

# Mallet composition file
input_file=$1
output_file=$2


cat $input_file | grep -v "^#" | cut -f 2,3,5,7,9,11,13,15,17,19,21 | grep -o '[^/]*$' | sed 's/%23/#/g'  > $output_file