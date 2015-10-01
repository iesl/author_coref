# Rexa Experiment #

The Rexa author disambiguation data set was presented in:

 - Culotta, Aron, et al. "Author disambiguation using error-driven machine learning with a ranking loss function." Sixth International Workshop on Information Integration on the Web (IIWeb-07), Vancouver, Canada. 2007.
 
To obtain a copy of the data set, please contact the owners of this repository.
 
## Set Up ##

In the data, folder you should find: 

```
data/rexa-topics/topics.db
data/rexa-keywords/keywords.db
data/rexa-embeddings/embeddings.txt
```

Place the Rexa data folder here:

```
data/rexa
```

## Running the Experiment ##

You can run the experiment with the script:

```
./scripts/coref/run_rexa_experiment.sh
```

This script has two config file:

```
RexaCoref.config
----------------

# The Rexa directory
--rexa-dir=data/rexa

# The file encoding
--codec=UTF-8

# The word embedding files
--embedding-dim=200
--embedding-file=data/rexa-embedding/embeddings.txt

# Number of threads (it is so small using 1 thread is fine)
--num-threads=1

# Output directory (for experimental results)
--output-dir=rexa-output

# Topics and keywords files
--topics-file=data/rexa-topics/topics.db
--keywords-file=data/rexa-keywords/keywords.db
```

and the feature tempalte weights:

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
...
```

The score will be displayed in the shell and result files will be written to the given output directory.



## Deterministic Baseline ##

To run the deterministic baseline, you can run the script:

```
./scripts/coref/run_rexa_deterministic_experiment.sh
```
 
 

