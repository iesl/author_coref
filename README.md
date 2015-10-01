# Author Disambiguation #

This project contains a implementation of a modified version of the Discriminative Hierarchical Coreference algorithm described in:

 - Wick, Michael, Sameer Singh, and Andrew McCallum. "A discriminative hierarchical model for fast coreference at large scale." Proceedings of the 50th Annual Meeting of the Association for Computational Linguistics: Long Papers-Volume 1. Association for Computational Linguistics, 2012.
 - Wick, Michael, Ari Kobren, and Andrew McCallum. "Large-scale author co-reference via hierarchical entity representations." Proceedings of the 30th International Conference on Machine Learning. 2013.

## Prerequisites ##

Required:

 - Java (1.7 or later)
 - Maven

Recommended:

 - MongoDB
 - Scala 

## Building the project ##

Use maven to compile and package the code:

```
mvn clean package
```

This will create a self contained jar with all of the project dependencies here:

```
target/author_coref-1.0-SNAPSHOT-jar-with-dependencies.jar 
```

This ```jar``` will be used to run the various components of the project.

To depend on this project, use maven to install the jar locally:

```
mvn install
```

and add the following to your ```pom.xml```:

```XML
<dependencies>
  ...
  <dependency>
  <groupId>edu.umass.cs.iesl.author_coref</groupId>
  <artifactId>author_coref</artifactId>
  <version>1.0-SNAPSHOT</version>
  <inceptionYear>2015</inceptionYear>
  </dependency>
  ...
</dependencies>
```

## Step by Step Guides ##

We provide instructions for re-running / reproducing Author Disambiguation on Rexa and ACL and provide some more pointers about using this package on your own data.

  - [Rexa](doc/REXA_EXPERIMENT.md)
  - [ACL](doc/ACL_EXPERIMENT.md)
  - [Custom Experiment](doc/CUSTOM_EXPERIMENT.md)

## Implementation Overview ##

Ambiguous author mentions are represented by the data structure ```AuthorMention```. This data structure serves as the input to the coreference algorithm and contains the fields used as features in the disambiguation.

An ```AuthorMention``` is defined as a ```Cubbie```, a serializable object interface from [factorie](https://github.com/factorie/factorie/blob/master/src/main/scala/cc/factorie/util/Cubbie.scala). It has the following fields:

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

To use author mentions of your own custom format. You can either write code to load your data directly into the ```AuthorMention``` objects, or, as was done for the Rexa and ACL data (shown in more detail below) define a intermediate data structure which is then converted into an ```AuthorMention```.


These ```AuthorMention``` data structures are disambiguated by implementations of the ```CoreferenceAlgorithm``` trait. 
 
The ```CoreferenceAlgorithms``` have the following functionality:
 
```Scala
 trait CoreferenceAlgorithm[MentionType <: CorefMention] {
 
   val name = this.getClass.ordinaryName
 
   /**
    * The mentions known to the algorithm
    * @return
    */
   def mentions: Iterable[MentionType]
 
   /**
    * Run the algorithm 
    */
   def execute(): Unit
   
   /**
    * Run the algorithm using numThreads
    * @param numThreads - number of threads to use
    */
   def executePar(numThreads: Int)
 
 
   /**
    * Return pairs of mentionIds and entityIds
    * @return
    */
   def clusterIds: Iterable[(String,String)]
 }
```

Implementations of this algorithm include the Hierarchical method at the focus of the project and a baseline deterministic approach. The constructor the hierarchical method is easy to use:


```Scala
class HierarchicalCoreferenceAlgorithm(opts: AuthorCorefModelOptions, override val mentions: Iterable[AuthorMention], keystore: Keystore, canopyFunctions: Iterable[AuthorMention => String]) extends CoreferenceAlgorithm[AuthorMention] with IndexableMentions[AuthorMention]
```

The following pseudocode is meant to express its usage:


```Scala
  def main(args: Array[String]): Unit = {
    val opts = new AuthorCorefModelOptions
    opts.parse(args)
    
    // Load the mentions 
    val mentions = LoadAuthorMentions(authorMentionsFile)
    
    // Load the word embeddings
    val keystore = InMemoryKeystore.fromFile(new File(keystorePath),keystoreDim,keystoreDelim,codec)
    
    // Define the canopy functions 
    val canopyFunctions = Iterable((a:AuthorMention) => Canopies.fullName(a), (a:AuthorMention) => Canopies.lastAndFirstNofFirst(a.self.value,3))

    // Initialize the algorithm
    val algorithm = new HierarchicalCoreferenceAlgorithm(opts,authorMentions,keystore,canopyFunctions)
    
    // Run the algorithm
    algorithm.execute()
    
    // Print the results
    println(algorithm.clusterIds.mkString("\n"))
  }
```


The downside to the ```CoreferenceAlgorithm``` framework is that it requires that all of the mentions fit into memory. The alternative ```ParallelCoreference``` framework instead load the mentions as needed from storage on disk.
 

The ```ParallelCoreference``` trait stores a listing of ```CorefTask``` objects:

```Scala
def allWork: Iterable[CorefTask]
```

The ```CorefTask``` objects store a list of mention ids that will be used to retrieve the ```AuthorMention``` objects:

```Scala
case class CorefTask(name: String, ids: Iterable[String])
```

The ```ParallelCoreference``` trait instantiates a thread pool of worker threads that operate over the ```CorefTask``` objects. Each ```CorefTask``` is handled in the same way:
 
```Scala
def handleTask(task: CorefTask): Unit = {
    // Fetch the mentions of the task
    val taskWithMentions = getMentions(task)
    // Create a new instance of a CoreferenceAlgorithm to process the mentions
    val alg = algorithmFromTask(taskWithMentions)
    // Process the mentions
    runCoref(alg,getMentions(task))
    // Write the results
    val wrtr = writer
    wrtr.write(task, alg.clusterIds)
  }
```

Currently the code is set up to use MongoDB to store the mentions on disk. The ```AuthorMention``` objects are directly serializable to MongoDB since they extend the ```Cubbie``` class. The wrapper to the MongoDB is defined in the class ```AuthorMentionDB```. This class extends the general interface to Mongo, ```MongoDatastore```, which provides functionality such as:

```Scala
  def bufferedInsert(cubbies: Iterator[T], bufferSize: Int) 
  def addIndices()
```

The ```AuthorMentionDB``` has an index on the ```mentionID``` field; it can be queried using the following method:

```Scala
override def get(key: String)
```
