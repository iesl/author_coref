# ACL Author Coreference #

This is a guide for running author coreference on ACL paper headers.

## Prerequisites ##

Currently the only supported markup of ACL papers with this project is [Grobid](https://github.com/kermitt2/grobid). This will be expanded in the near future to [RPP](https://github.com/iesl/rpp) and [ParsCit](https://github.com/knmnyn/ParsCit). 

Begin by running Grobid on the ACL papers and place the resulting XML files in the folder ```data/acl-grobid```. (Note that another location may be used, but using a simple relative path like this is used for clarity here). 
 
## Setting up the Database of Author Mentions ##

### Starting Mongo DB ###

We will be storing the ACL author mentions in a MongoDB instance. Let's begin by starting the Mongo server. To do this run the script,

```
./scripts/db/start-mongo-server.sh
```

The script will start a MongoDB instance will now be running on port 25752 on your machine. In this guide, the MongoDB will be running on the same machine as the coreference algorithm. To change this, simply change the hostname of the Mongo server in the files in the ```config``` directory. 

Note: This script assumes that the machine that is being used is a NUMA machine. If you are not using a NUMA machine, you may start the database by using the command: 

```
mongod --port 25752 --dbpath data/mongodb/authormention_db
```

### Populating the Database ###

The next step is to populate the database with author mentions. The author mentions are represented by the data structure ```AuthorMention```. The main project read me has more information on this data structure. To do this, we will process the Grobid XML files, extracting paper header information and use this to generate the author mentions. We will also incorporate additional information from the abstract texts of the papers: topic information discovered using LDA and keyword phrases from the text discovered using Pointwise Mutual Information. This additional information is stored in the data files:

```
data/acl-topics/topics.db
data/acl-keywords/keywords.db
```

We populate the database using the script:

```
./scripts/db/populate-acl-mentions.sh
```

The configuration for this script is defined in ```config/db/PopulateACLMentions.config```:

```
--acl-dir=data/acl-grobid
--acl-file-codec=ISO-8859-1
--topics-file=data/acl-topics/topics.db
--keywords-file=data/acl-keywords/keywords.db
--hostname=localhost
--port=25752
--dbname=authormention_db
--collection-name=authormention
--num-threads=18
--buffered-size=1000
```

These fields can be updated as necessary. The script calls the Scala class: ```PopulateAuthorMentionDBWithACLMentions```, which loads the header information for a given paper and generates a mention for each author listed in the header. The mentions are then joined with the additional topic and keyword information.

This script will populate the database in about 15 seconds. 

## Running Coreference ##

### Preparing the Coreference Input ###

For efficiency, we precompute a blocking/canopy partition of the data so that blocks of mentions can be processed in parallel. We call this procedure generating ```CorefTasks```.  Each ```CorefTask``` object contains the ids of a set of mentions which are possibly coreferent. The actual mentions corresponding to these ids are then loaded at exactly the moment the ```CorefTask``` has been scheduled to be processed by one of the many coreference worker threads. To generate the ```CorefTasks`` file for the ACL data, run the script: 
  
```
./scripts/coref/create_coref_tasks_acl.sh 
```

The configuration for this script is in ```config/coref/CreateCorefTasks.config```: 

```
--acl-dir=data/acl-grobid
--acl-file-codec=ISO-8859-1
--output-file=data/acl-coref-tasks.tsv
--num-threads=4
```

This script calls the ```GenerateCorefTasksFromACL``` Scala class which uses a predefined canopy definition to determine the tasks.

The result of this a file ```data/acl-coref-tasks.tsv``` with format: 

```
Task Name                  Comma separated mention ids
LAST_liu_FIRST_hon      P05-3005_LN_Liu_FN_Hongfang,S13-1021_LN_Liu_FN_Hongfang,W04-3104_LN_Liu_FN_Hongfang
LAST_liu_FIRST_hui      C10-2082_LN_Liu_FN_Huidan,W06-1624_LN_Liu_FN_Hui,W07-1110_LN_Liu_FN_Hui
LAST_liu_FIRST_yi       C08-1093_LN_Liu_FN_Yi,P07-1047_LN_Liu_FN_Yi,P07-1059_LN_Liu_FN_Yi
LAST_liu_FIRST_yua      C08-1063_LN_Liu_FN_Yuanjie,P13-4012_LN_Liu_FN_Yuanchao,W10-4159_LN_Liu_FN_Yuanchao
```

### Performing Disambiguation ###

Now we are ready to execute the coreference algorithm. This can be done by using the script:
 
```
./scripts/coref/run_coref_acl.sh
```

This uses two configuration files. The first specifies the connection details to the Mongo instance, the file path to the word embeddings (which are used by feature templates), etc:

```
ACLCoref.config
---------------
# Where to find the coref task file, which specifies the separate coref jobs for execution
--coref-task-file=data/acl-coref-tasks.tsv

# Where to write the coref output
--output-dir=data/acl-coref-output

# The number of threads to use
--num-threads=18

# The file encoding
--codec=UTF-8

# The embeddings
--embedding-dim=200
--embedding-file=data/acl-embedding/embeddings.txt

# MongoDB
--hostname=localhost
--port=25752
--dbname=authormention_db
--collection-name=authormention
```

and a second which gives the feature template weights:

```
DefaultWeights.config
---------------------
# First names
--model-author-bag-first-initial-weight=20.0
--model-author-bag-first-noname-penalty=0.0
--model-author-bag-first-name-weight=4.0
--model-author-bag-first-saturation=40.0
--model-author-bag-first-weight=1.0

# Middle Names
--model-author-bag-middle-initial-weight=8.0
--model-author-bag-middle-noname-penalty=0.0
--model-author-bag-middle-name-weight=3.0
--model-author-bag-middle-saturation=40.0
--model-author-bag-middle-weight=1.0

# Emails
--model-author-bag-emails-weight=20.0
--model-author-bag-emails-shift=0.0
--model-author-bag-emails-entropy=0.0
--model-author-bag-emails-prior=0.0

# Institutions
--model-author-bag-institutions-weight=8.0
--model-author-bag-institutions-shift=0.0
--model-author-bag-institutions-entropy=0.0
--model-author-bag-institutions-prior=0.0

...
```

The script calls the ```RunParallelCoreferenceACL``` class which executes the coreference code using an implementation of the ```ParallelCoreference``` trait. 

This coreference process takes about 1 minute.

The dismabiguation results are written to a file ```data/acl-coref-output/all-results.txt``` with the format:

```
Mention ID  Entity ID
A00-1001_LN_Amble_FN_Tore       1
A00-1002_LN_Haji_FN_Jan 2
A00-1002_LN_Hric_FN_Jan 3
A00-1003_LN_Flank_FN_Sharon     4
A00-1004_LN_Chen_FN_Jiang       5
A00-1004_LN_Nie_FN_Jian-Yun     6
A00-1005_LN_Bagga_FN_Amit       7
A00-1005_LN_Bowden_FN_G 8
```

Note: The mention IDs used are the paper id contactenated with the last name and first name of the authors.