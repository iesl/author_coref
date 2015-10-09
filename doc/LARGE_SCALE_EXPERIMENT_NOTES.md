# Large Scale Experiment Notes #

This guide will show how to use our system along with MongoDB to run author disambiguation on tens (or hundreds) of millions of mentions.

To start, we show how to set up a single MongoDB instance which will be queried by all worker threads. In the future, we can replicate or shard the database for better performance.

Please email me with any questions/issues you encounter.

## Recommended Machine Setup ##

 - 40+ GB RAM
 - 20+ CPUs
 - [NUMA](https://en.wikipedia.org/wiki/Non-uniform_memory_access) architecture (untested without this)
 - MongoDB 3.0+ installed following any additional instructions Mongo gives for your OS.

## Starting Mongo ##

Create a directory in which to store the database, e.g.: 

```
mkdir -p data/mongodb/authormention_db
```

To start mongo on a NUMA machine use the following command:

```
numactl --interleave=all mongod --port 25752 --dbpath data/mongodb/authormention_db
```

and to start mongo on a non-NUMA machine:

```
mongod --port 25752 --dbpath data/mongodb/authormention_db
```

This will create a running MongoDB instance on the given machine and on port 25752. We will then populate the database with mentions using a Scala program described below. 

The shell script ```scripts/db/start-mongo-server.sh``` can be used.

## Data Format ##

The coreference algorithms are designed to operate on ```AuthorMention```  data structures. The ```AuthorMention``` data structures extend [Cubbie](https://github.com/factorie/factorie/blob/master/src/main/scala/cc/factorie/util/Cubbie.scala), a serializable object from [factorie](https://github.com/factorie/factorie/). Cubbies can be serialized and deserialized easily to and from MongoDB. The ```AuthorMention```s have the following fields:


```Scala
class AuthorMention extends CorefMention {
  
  // The author in focus for this mention
  val self = new CubbieSlot[Author]("self", () => new Author())
  // The co-authors listed on the publication
  val coauthors = new CubbieListSlot[Author]("coauthors",() => new Author())
  // The title of the publication
  val title = new StringSlot("title")
  // The words of the title that will be used in the word embedding representation of the title
  val titleEmbeddingKeywords = new StringListSlot("titleEmbeddingKeywords")
  // The topics discovered using LDA or similar method
  val topics = new StringListSlot("topics")
  // The raw string text containing the abstract and/or body of the publication
  val text = new StringSlot("text")
  // A tokenized version of the text
  val tokenizedText = new StringListSlot("tokenizedText")
  // The venue(s) of publication
  val venues = new CubbieListSlot[Venue]("venues", () => new Venue())
  // The keywords of the publication
  val keywords = new StringListSlot("keywords")
  
  // The canopies to which the mention belongs
  val canopies = new StringListSlot("canopies")
  // For cases where the mention belongs to a single canopy
  val canopy = new StringSlot("canopy")
  // Where the mention came from
  val source = new StringSlot("source")
...
}
```

The ```AuthorMention``` inherits the following fields from its parent class ```CorefMention```:

```Scala
trait CorefMention extends CubbieWithHTMLFormatting {
  def mentionId: StringSlot = new StringSlot("mentionId")
  def entityId: StringSlot = new StringSlot("entityId")
}
```

Note that the algorithms can be used on other data structures with some minor changes.

## Loading Data ##

Given a collection of ambiguous author records, in order to use this system and populate the MongoDB, we first need to be able to load (or convert) the data into the ```AuthorMention``` data structures. This can be done in many ways. 

### JSON ###


One possible way is to use JSON serialized data. The ```AuthorMention``` records are JSON serializable and deseriazable. The schema of the JSON object aligns exactly with the schema defined by the "slots" in the ```AuthorMention```. For instance the JSON representation might look like:

```JSON
{ 
    "mentionId" : "P81-1005_LN_Becket_FN_Lee",
    "titleEmbeddingKeywords" : [ "phony", "a", "heuristic", "phonological", "analyzer" ],
    "topics" : [ ],
    "coauthors" : [ ], 
    "title" : "PHONY: A Heuristic Phonological Analyzer*", 
    "self" : { 
        "middleNames" : [ "a" ], 
        "lastName" : "becket", 
        "firstName" : "lee" 
    }, 
    "tokenizedText" : [ ],
    "keywords" : [ ] 
}
```

The key for each JSON key-value pair is exactly the name specified in each of the above slot constructors. The value of the JSON pairs has a data type corresponding to the slot field type. Empty fields can be specified either with empty values or can be omitted. A full specification of the JSON structure is available [here](data_structures/JSON.md).

The class ```LoadJSONAuthorMentions``` provides methods for loading JSON serialized mentions from files. The files must store 1 AuthorMention per line.

```Scala
object LoadJSONAuthorMentions {

  // Given a file with 1 JSON Author Mention per line, load the mentions in a single stream
  def load(file: File, codec: String): Iterator[AuthorMention]
  
  
  // Given a file with 1 JSON Author Mention per line, splits the file into multiple groups
  // loads the groups in parallel
  def loadMultiple(file: File, codec: String, num: Int): Iterable[Iterator[AuthorMention]]

}
```

### Other Formats ###

Alternatively, you can write a custom loader for your data. You may either write a loader that loads directly in the ```AuthorMention``` format or you may define an intermediate data structure that is then converted into the ```AuthorMention``` format. The second option is done for the Rexa and ACL data. There are ```RexaAuthorMention```s and ```ACLAuthorMention```s which are then converted into ```AuthorMention```s using the classes ```GenerateAuthorMentionsFromRexa``` and ```GenerateAuthorMentionsFromACL``` respectively. You may find it helpful to use these classes as a template. 

## Populating the MongoDB ##

### General Method ###

Once you have your data accessible in the ```AuthorMention``` format, you may load it into the MongoDB. This can be done with the following commands:

```Scala
val yourMentions: Iterator[AuthorMentions] = loadMentions()
val db = new AuthorMentionDB(host,port,dbname,collection_name,ensureIndices=false)
PopulateAuthorMentionDB.insert(yourMentions,db,bufferSize=1000)
```

You can parallelize the insert if you have access to multiple iterators of ```AuthorMentions```:

```Scala
val yourMentionStreams: Iterable[Iterator[AuthorMentions]] = loadMentionsPar()
val db = new AuthorMentionDB(host,port,dbname,collection_name,ensureIndices=false)
PopulateAuthorMentionDB.insertPar(yourMentionStreams,db,bufferSize=1000)
```

After you insert the mentions into the database, make sure to create the index on the ```mentionId``` field:

```Scala
db.addIndices()
```

### Using JSON Loader ###

The script ```scripts/db/populate-json-mention.sh``` loads JSON formatted mentions into a MongoDB. 

```Bash
#!/bin/sh

jarpath="target/author_coref-1.0-SNAPSHOT-jar-with-dependencies.jar"

time java -Xmx40G -cp $jarpath edu.umass.cs.iesl.author_coref.db.PopulateAuthorMentionDBFromJSON \
--config=config/db/PopulateJSONMentions.config
```

The config file ```config/db/PopulateJSONMentions.config``` can be edited to fit your requirements. It has the following contents:

```
--json-file=data/mentions.json
--codec=UTF-8
--hostname=localhost
--port=25752
--dbname=authormention_db
--collection-name=authormention
--num-threads=18
--buffered-size=1000
```

The shell script calls the Scala class: ```PopulateAuthorMentionDBFromJSON```, which functions in a very similar way to the steps described above:

```Scala
object PopulateAuthorMentionDBFromJSON {
  def main(args: Array[String]): Unit = {
    val opts = new PopulateAuthorMentionDBFromJSONOpts()
    opts.parse(args)
    val mentions = LoadJSONAuthorMentions.loadMultiple(new File(opts.jsonFile.value),opts.codec.value,opts.numThreads.value)
    val db = new AuthorMentionDB(opts.hostname.value,opts.port.value,opts.dbname.value,opts.collectionName.value,false)
    PopulateAuthorMentionDB.insertPar(mentions,db,opts.bufferSize.value)
    db.addIndices()
  }
}
```

## Generating the Coref Tasks ##

### General Method ###

To run parallel coreference, you need to specify the canopy/blocking structure of your mentions. To do this, you generate a ```CorefTask``` file. This is done for efficiency, so that blocks of mentions can be processed in parallel. Each ```CorefTask``` object contains the ids of a set of mentions which are possibly coreferent. The actual mentions corresponding to these ids are then loaded at exactly the moment the ```CorefTask``` has been scheduled to be processed by one of the many coreference worker threads. 


The general way to determine the Coref Tasks is:

```Scala
val yourMentionStreams: Iterable[Iterator[AuthorMentions]] = loadMentionsPar()
// Your choice of canopy function, but make sure it is consistent with the choice of function you use in your coreference algorithm in the next step.
val canopyAssignment = (a: AuthorMention) => Canopies.lastAndFirstNofFirst(a.self.value,3)
// Generate the tasks (disregard the last argument-- its use case is not important here
val tasks = GenerateCorefTasks.fromMultiple(authorMentionStreams,canopyAssignment,Set())
// Write to a file
GenerateCorefTasks.writeToFile(tasks,new File(opts.outputFile.value))
```

The result of this a file with format: 

```
Task Name                  Comma separated mention ids
LAST_liu_FIRST_hon      P05-3005_LN_Liu_FN_Hongfang,S13-1021_LN_Liu_FN_Hongfang,W04-3104_LN_Liu_FN_Hongfang
LAST_liu_FIRST_hui      C10-2082_LN_Liu_FN_Huidan,W06-1624_LN_Liu_FN_Hui,W07-1110_LN_Liu_FN_Hui
LAST_liu_FIRST_yi       C08-1093_LN_Liu_FN_Yi,P07-1047_LN_Liu_FN_Yi,P07-1059_LN_Liu_FN_Yi
LAST_liu_FIRST_yua      C08-1063_LN_Liu_FN_Yuanjie,P13-4012_LN_Liu_FN_Yuanchao,W10-4159_LN_Liu_FN_Yuanchao
```

### Using JSON ###


To generate the coref tasks from a file of JSON mentions, you can use the script:

```
./scripts/coref/create_coref_tasks_json.sh
```

The script calls the Scala class ```GenerateCorefTasksFromJSON```


```Bash
#!/bin/sh

jarpath="target/author_coref-1.0-SNAPSHOT-jar-with-dependencies.jar"

time java -Xmx20G -cp $jarpath edu.umass.cs.iesl.author_coref.process.GenerateCorefTasksFromJSON \
--config=config/coref/CreateCorefTasksJSON.config
```

The configuration of the script can be edited in the ```config/coref/CreateCorefTasksJSON.config``` config file. Its contents are:

```
--json-file=data/mentions.json
--output-file=data/coref-tasks.tsv
--num-threads=18
```

The class ```GenerateCorefTasksFromJSON``` is defined in a similar way to the above description:

```Scala
object GenerateCorefTasksFromJSON {
  def main(args: Array[String]): Unit = {
    val opts = new GenerateCorefTasksFromJSONOpts()
    opts.parse(args)
    val mentions = LoadJSONAuthorMentions.loadMultiple(new File(opts.jsonFile.value),opts.codec.value,opts.numThreads.value)
    val canopyAssignment = (a: AuthorMention) => Canopies.lastAndFirstNofFirst(a.self.value,3)
    val ids = if (opts.idRestrictionsFile.wasInvoked) Source.fromFile(opts.idRestrictionsFile.value,opts.codec.value).getLines().toIterable.toSet[String] else Set[String]()
    val tasks = GenerateCorefTasks.fromMultiple(mentions,canopyAssignment,ids)
    GenerateCorefTasks.writeToFile(tasks,new File(opts.outputFile.value))
  }
}
```

## Word Embeddings ##

### Preparing Training Data ###

#### General Method ####

Another requirement of the system is to have a word embedding model trained on the data. To do this, we need to generate training data for the model. This is done by gathering the text of our ```AuthorMentions``` and performing some normalization and writing it out to a file. For instance:

```Scala
val mentions = loadMentions()
val texts = GenerateWordEmbeddingTrainingData.getTexts(mentions,opts.removePunctuation.value,opts.lowercase.value)
val writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(opts.outputFile.value)),opts.codec.value))
texts.foreach{
  t => 
    writer.write(t)
    writer.write("\n")
    writer.flush()
}
writer.close()
```


#### Using JSON ####


You can use the following script to generate the the training data from JSON serialized mentions:

```
./scripts/embedding/generate-embedding-training-data-from-json.sh
```

The script calls the class ```GenerateWordEmbeddingTrainingDataFromJSON```:

```Bash
#!/bin/sh

jarpath="target/author_coref-1.0-SNAPSHOT-jar-with-dependencies.jar"

time java -Xmx20G -cp $jarpath edu.umass.cs.iesl.author_coref.embedding.GenerateWordEmbeddingTrainingDataFromJSON \
--config=config/embedding/GenerateWordEmbeddingTrainingDataFromJSON.config
```
 
With the following config file:

```
--json-file=data/mentions.json
--output-file=data/embedding/training_data.txt
--remove-punctuation=true
--lowercase=true
```

### Training Embeddings ###

To train the embeddings, using a Word2Vec like model, use the following script:

```
./scripts/embedding/train-embeddings.sh
```

The script uses a word embedding implementation in factorie:

```Bash
#!/bin/sh
echo "Training word embeddings"
START_TIME=$(date +%x_%H:%M:%S:%N)
START=$(date +%s)
jarpath="target/author_coref-1.0-SNAPSHOT-jar-with-dependencies.jar"

# Edit these settings if you need to
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
```


## Running Disambiguation ##

Now we have generated the required inputs to the disambiguation system. The program which runs the parallel coreference takes the following settings: 

```
# Where to find the coref task file, which specifies the separate coref jobs for execution
--coref-task-file=data/coref-tasks.tsv

# Where to write the coref output
--output-dir=data/coref-output

# The number of threads to use
--num-threads=18

# The file encoding
--codec=UTF-8

# The word embeddings
--embedding-dim=200
--embedding-file=data/embedding/embeddings.txt

# MongoDB
--hostname=localhost
--port=25752
--dbname=authormention_db
--collection-name=authormention
```

An example program for parallel disambiguation is:
 
The code snippet for this is:

```Scala
object MyRunParallel {

  def main(args: Array[String]): Unit = {

    // Uses command line options from factorie
    val opts = new RunParallelOpts
    opts.parse(args)

    // Load all of the coref tasks into memory, so they can easily be distributed amongst the different threads
    val allWork = LoadCorefTasks.load(new File(opts.corefTaskFile.value),opts.codec.value).toIterable

    // Create the interface to the MongoDB containing the mentions
    val db = new AuthorMentionDB(opts.hostname.value, opts.port.value, opts.dbname.value, opts.collectionName.value, false)
    
    // The lookup table containing the embeddings. 
    val keystore = InMemoryKeystore.fromFile(new File(opts.keystorePath.value),opts.keystoreDim.value,opts.keystoreDelim.value,opts.codec.value)

    // Create the output directory
    new File(opts.outputDir.value).mkdirs()
    
    // You may define the canopy function in any way you choose, right now if you use more than one function, the canopies must become increasingly general as in the below example.
    val canopyFunctions = Iterable((a:AuthorMention) => Canopies.fullName(a.self.value),(a:AuthorMention) => Canopies.firstAndLast(a.self.value), (a:AuthorMention) => Canopies.lastAndFirstNofFirst(a.self.value,3))

    // Initialize the coreference algorithm
    val parCoref = new ParallelHierarchicalCoref(allWork,db,opts,keystore,canopyFunctions,new File(opts.outputDir.value))

    // Run the algorithm on all the tasks
    parCoref.runInParallel(opts.numThreads.value)

    // Write the timing info
    val timesPW = new PrintWriter(new File(opts.outputDir.value,"timing.txt"))
    timesPW.println(parCoref.times.map(f => f._1 + "\t" + f._2).mkString(" "))
    timesPW.close()

    // display the timing info
    parCoref.printTimes()
  }
}
```

## Distributing Coreference ##

The simplest (though perhaps too naive) way to distribute the coreference work is to split up the coref task file into several chunks and to run a parallel coreference job with a different task file on each machine. One machine will run the MongoDB instance-- make sure to specify the name of this machine instead of localhost in the config files. 
