#!/bin/sh


echo "Starting Mongo Server on port 25752 at $START_TIME"
mkdir -p data/mongodb/authormention_db
numactl --interleave=all mongod --port 25752 --dbpath data/mongodb/authormention_db
