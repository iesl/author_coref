# Running Disambiguation on New Data #

## Loading Data ##

The first step to using this disambiguation code on your own data is to define a way to load your data and to convert it into the ```AuthorMention``` format. You may either write a loader that loads directly in the ```AuthorMention``` format or you may define an intermediate data structure that is then converted into the ```AuthorMention``` format. The second option is done for the Rexa and ACL data. There are ```RexaAuthorMention```s and ```ACLAuthorMention```s which are then converted into ```AuthorMention```s using the classes ```GenerateAuthorMentionsFromRexa``` and ```GenerateAuthorMentionsFromACL``` respectively. You may find it helpful to use these classes as a template. 

## Populating the MongoDB ##

Once you have your data accessible in the ```AuthorMention``` format, you may load it into the MongoDB. This can be done with the following commands:

```Scala
val yourMentions: Iterator[AuthorMentions] = loadMentions()
val db = new AuthorMentionDB(host,port,dbname,collection_name,ensureIndices=false)
PopulateAuthorMentionDB.insert(yourMentions,db,bufferSize=1000)
```

You can parallelize the insert if you access to multiple iterators of ```AuthorMentions```:

```Scala
val yourMentionStreams: Iterable[Iterator[AuthorMentions]] = loadMentionsPar()
val db = new AuthorMentionDB(host,port,dbname,collection_name,ensureIndices=false)
PopulateAuthorMentionDB.insertPar(yourMentionStreams,db,bufferSize=1000)
```

After you insert the mentions into the database, make sure to create the index on the ```mentionId``` field:

```Scala
db.addIndices()
```

You can see an example of this for the ACL data in ```PopulateAuthorMentionDBWithACLMentions```.

## Generating the Coref Tasks ##

To run the parallel coreference you need to generate a Coref Task file as described in the ACL experiment guide. This is very simple once your data is in the ```AuthorMention``` format:


```Scala
val yourMentionStreams: Iterable[Iterator[AuthorMentions]] = loadMentionsPar()
// Your choice of canopy function, but make sure it is consistent with the choice of function you use in your coreference algorithm in the next step.
val canopyAssignment = (a: AuthorMention) => Canopies.lastAndFirstNofFirst(a.self.value,3)
// Generate the tasks (disregard the last argument-- its use case is not important here
val tasks = GenerateCorefTasks.fromMultiple(authorMentionStreams,canopyAssignment,Set())
// Write to a file
GenerateCorefTasks.writeToFile(tasks,new File(opts.outputFile.value))
```

You can see an example of this in the ```GenerateCorefTasksFromACL``` class.


## Running Disambiguation ##

The execution of the coreference algorithm will take the coref task file as input along with the mongo db settings and other options and run the disambiguation in parallel.
 
The code snippet for this is:


```Scala
object MyRunParallel {

  def main(args: Array[String]): Unit = {

    // Uses command line options see ACL config files for an example of what you need to specify
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


## Retraining Embeddings, Topics and Keywords ##

Recomputing each of these is entirely possible. Guides for doing this will be posted in the next few days.

