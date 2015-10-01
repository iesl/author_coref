#!/bin/sh


input=$1
output=$2

cut -f 1 $input | sort -u > $output

