/* Copyright (C) 2015 University of Massachusetts Amherst.
   This file is part of “author_coref”
   http://github.com/iesl/author_coref
   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0
   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License. */

package edu.umass.cs.iesl.author_coref.coreference


import java.io.{File, PrintWriter}
import java.util
import java.util.Date

import cc.factorie.util.Threading
import edu.umass.cs.iesl.author_coref.data_structures.coreference.{CorefMention, AuthorMention, CorefTaskWithMentions, CorefTask}
import edu.umass.cs.iesl.author_coref.db.Datastore
import edu.umass.cs.iesl.author_coref.load.LoadTabSeparatedTuples
import edu.umass.cs.iesl.author_coref.process.{CaseInsensitiveReEvaluatingNameProcessor, NameProcessor}

import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer

/**
  * The base trait for running coreference in multiple threads.
  * Parallelizing across canopies.
  */
trait ParallelCoreference {

  /**
    * Keeps track of the running times
    */
  val times = new ArrayBuffer[(String,String)]()

  /**
    * All of the canopy groups that need to be processed
    * @return
    */
  def allWork: Iterable[CorefTask]

  /**
    * The mechanism for outputting the coreference groups
    * @return
    */
  def writer: CorefOutputWriter

  /**
    * The name processor to apply to the mentions (if necessary)
    * @return
    */
  def nameProcessor: NameProcessor

  /**
    * Given a task, fetch the algorithm that will be used to run it
    * @param task - the task
    * @return
    */
  def algorithmFromTask(task: CorefTaskWithMentions): CoreferenceAlgorithm[AuthorMention]

  /**
    * Run the disambiguation algorithm on the given task.
    * @param alg - the algorithm
    * @param task - the task
    */
  def runCoref(alg: CoreferenceAlgorithm[AuthorMention], task: CorefTaskWithMentions) = alg.execute()

  /**
    * Fetch the AuthorMentions associated with a given task
    * @param corefTask - the task
    * @return - the task with mentions
    */
  def getMentions(corefTask: CorefTask):CorefTaskWithMentions

  /**
    * Given a task, fetch the mentions, generate the algorithm instance, run the task
    * and write out the results.
    * @param task
    */
  def handleTask(task: CorefTask): Unit = {
    val wrtr = writer
    val taskWithMentions = getMentions(task)
    val alg = algorithmFromTask(taskWithMentions)
    runCoref(alg,taskWithMentions)
    wrtr.write(task, alg.clusterIds,taskWithMentions.mentions,(m) => m.tsvString)
  }

  /**
    * Distribute the coref tasks across many threads
    * @param numThreads - number of threads to use
    * @return
    */
  def runInParallel(numThreads: Int) = {
    val corefStart = System.currentTimeMillis()
    times.+=(("Coreference Starting Timestamp", new Date(System.currentTimeMillis()).toString))
    Threading.parForeach(allWork.zipWithIndex,numThreads)(
      task => {
        if (task._2 % 1000 == 0)
          println(s"[ParallelCoreference] Thread ID#: ${Thread.currentThread().getId} Completed about ${task._2} tasks")
        handleTask(task._1)
      })
    val corefTime = System.currentTimeMillis() - corefStart
    times.+=(("Coreference Time", corefTime.toString))
    times.+=(("Coreference Ending Timestamp", new Date(System.currentTimeMillis()).toString))
    println(s"[ParallelCoreference] Finished Coreference in $corefTime ms. Finalizing output.")
    times.+=(("Finalization Starting Timestamp", new Date(System.currentTimeMillis()).toString))
    val finalizationStart = System.currentTimeMillis()
    finalizeOutput()
    val finalizationTime = System.currentTimeMillis() - finalizationStart
    times.+=(("Finalization Time",finalizationTime.toString))
    times.+=(("Finalization Ending Timestamp", new Date(System.currentTimeMillis()).toString))
  }

  /**
    * Collect the output and format it in some way
    */
  def finalizeOutput(): Unit

  /**
    * Display the running time information
    */
  def printTimes() = times.foreach(f => println(f._1 + ":\t" + f._2))

}

/**
  * Load mentions from a mongo db based on ids
  */
trait LoadMentionsFromMongo {

  val datastore: Datastore[String,AuthorMention]

  def load(ids: Iterable[String]) = ids.map(datastore.get(_).head)

}

/**
  * Base class for the distributed coreference
  * @param allWork - the complete group of tasks
  * @param datastore - the interface to mongo
  * @param outputDir - where to write the output
  */
abstract class StandardParallelCoreference(override val allWork: Iterable[CorefTask],
                                           override val datastore: Datastore[String, AuthorMention],
                                           outputDir: File,debug: Boolean) extends ParallelCoreference with LoadMentionsFromMongo {

  override def writer: CorefOutputWriter = new TextCorefOutputWriter(outputDir,debug = debug)

  override def finalizeOutput(): Unit = writer.collectResults(outputDir,new File(outputDir,"all-results.txt"))

  override def getMentions(corefTask: CorefTask): CorefTaskWithMentions = new CorefTaskWithMentions(corefTask.name,corefTask.ids,load(corefTask.ids))

}


/**
  * The implementation using multiple canopies
  * @param allWork - the complete group of tasks
  * @param datastore - the interface to mongo
  * @param opts - the model parameters
  * @param keystore - the embedding lookup table
  * @param canopyFunctions - the canopy functions to use
  * @param nameProcessor - the name processor to use
  * @param outputDir - where to write the output
  */
class ParallelHierarchicalCoref(override val allWork: Iterable[CorefTask],
                                override val datastore: Datastore[String, AuthorMention],
                                opts: AuthorCorefModelOptions,
                                keystore: Keystore,
                                canopyFunctions: Iterable[(AuthorMention => String)],
                                outputDir: File,
                                override val nameProcessor: NameProcessor = CaseInsensitiveReEvaluatingNameProcessor,
                                debug: Boolean = false) extends StandardParallelCoreference(allWork,datastore,outputDir,debug) {
  override def algorithmFromTask(task: CorefTaskWithMentions): CoreferenceAlgorithm[AuthorMention] = {
    val alg = new HierarchicalCoreferenceAlgorithm(opts,task.mentions,keystore,canopyFunctions,nameProcessor)
    alg.quietPrintStatements = true
    alg
  }
}

/**
  * Interface for writing the coreference output
  */
trait CorefOutputWriter {

  def write(task: CorefTask, results: Iterable[(String,String)])

  def write(task: CorefTask, results: Iterable[(String,String)], mentions: Iterable[CorefMention], mentionToString: (CorefMention) => String)

  def collectResults(outputDir: File, collectedFile: File)

}

object CorefOutputWriterHelper {

  def sortedNormalizedResults(results: Iterable[(String,String)]): Iterable[(String,String)] = {
    // 1. Create the entity id map
    val sortedResults = results.toSeq.sortBy(_._1)
    var idx = 1
    val entityIdMap = new util.HashMap[String,String](100000).asScala
    sortedResults.foreach{
      case ((mentionId,entityId)) =>
        if (!entityIdMap.contains(entityId)) {
          entityIdMap.put(entityId,idx.toString)
          idx += 1
        }
    }
    // 2.  Sort the mention ids
    sortedResults.map(f => (f._1,entityIdMap(f._2)))
  }

}

/**
  * Implementation of CorefOutput interface which writes the coref tasks results each to their own file
  * in a given directory. A subdirectory structure is create so that "ls" does not take too long
  * @param outputDirectory - the output directory
  * @param codec - the encoding of the files (default UTF8)
  */
class TextCorefOutputWriter(outputDirectory: File, codec: String = "UTF-8", debug: Boolean = false) extends CorefOutputWriter {

  val validFilePathRegex = "^[1234567890qwertyuiopasdfghjklzxcvbnmQWERTYUIOPASDFGHJKLZXCVBNM\\._]+$"

  def prefix(task: CorefTask) = if (task.name.matches(validFilePathRegex)) task.name.slice(0,7) else "misc"

  val extension = ".coref"

  def addExtension(string: String) = string + extension

  override def write(task: CorefTask, results: Iterable[(String, String)]): Unit = {
    val subDirName = prefix(task)
    new File(outputDirectory,subDirName).mkdirs()
    val filename = addExtension(if (task.name.matches(validFilePathRegex)) task.name else task.name.hashCode.toString)
    val pw = new PrintWriter(new File(new File(outputDirectory,subDirName),filename),codec)
    results.foreach(f => {pw.println(f._1 + "\t" + f._2); pw.flush()})
    pw.close()
  }

  override def collectResults(outputDir: File, collectedFile: File): Unit = {
    val results = CorefOutputWriterHelper.sortedNormalizedResults(outputDir.list().filter(f => new File(outputDir,f).isDirectory).flatMap(f => loadAllResults(new File(outputDir,f))))
    val pw = new PrintWriter(collectedFile,codec)
    results.foreach(f => {pw.println(f._1 + "\t" + f._2); pw.flush()})
    pw.close()
  }

  def loadAllResults(directory: File) = {
    val results = new ArrayBuffer[(String,String)]
    directory.list().filter(_.endsWith(extension)).foreach{
      filename =>
        results ++= LoadTabSeparatedTuples.load(new File(directory,filename),codec)
    }
    results
  }

  override def write(task: CorefTask, results: Iterable[(String, String)], mentions: Iterable[CorefMention], mentionToString: (CorefMention) => String): Unit = {
    val subDirName = prefix(task)
    val mentionMap = if (debug) mentions.groupBy(_.mentionId.value).mapValues(_.head) else Map[String,CorefMention]()
    new File(outputDirectory,subDirName).mkdirs()
    val filename = addExtension(if (task.name.matches(validFilePathRegex)) task.name else task.name.hashCode.toString)
    val pw = new PrintWriter(new File(new File(outputDirectory,subDirName),filename),codec)
    if (debug)
      results.foreach(f => {pw.println(f._1 + "\t" + f._2 + "\t" + mentionToString(mentionMap(f._1))); pw.flush()})
    else
      results.foreach(f => {pw.println(f._1 + "\t" + f._2 + "\t"); pw.flush()})
    pw.close()
  }
}
