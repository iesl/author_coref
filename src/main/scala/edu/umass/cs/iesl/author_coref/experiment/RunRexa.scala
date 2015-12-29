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

package edu.umass.cs.iesl.author_coref.experiment

import java.io.File

import edu.umass.cs.iesl.author_coref.coreference._
import edu.umass.cs.iesl.author_coref.data_structures.coreference._
import edu.umass.cs.iesl.author_coref.db.{GenerateAuthorMentionsFromRexa, KeywordsDB, TopicsDB}
import edu.umass.cs.iesl.author_coref.load.{LoadKeywords, LoadRexa, LoadTopics}
import edu.umass.cs.iesl.author_coref.utilities.KeystoreOpts

class RunRexaOpts extends AuthorCorefModelOptions with KeystoreOpts {
  val rexaDir = new CmdOption[String]("rexa-dir", "The dir containing the rexa files", true)
  val codec = new CmdOption[String]("codec", "UTF-8", "STRING", "The encoding to use.")
  val outputDir = new CmdOption[String]("output-dir", "Where to write the output", true)
  val numThreads = new CmdOption[Int]("num-threads", 1, "INT", "Number of threads to use")
  val topicsFile = new CmdOption[String]("topics-file", "the topics file", false)
  val keywordsFile = new CmdOption[String]("keywords-file", "the keywords file", false)
}

object RunRexa {

  
  def main(args: Array[String]): Unit = {
    
    val opts = new RunRexaOpts
    opts.parse(args)

    println("Loading the topics database")
    val topicsDB = if (opts.topicsFile.wasInvoked) LoadTopics.load(new File(opts.topicsFile.value),opts.codec.value) else new TopicsDB(Map())
    val keywordsDB = if (opts.keywordsFile.wasInvoked) LoadKeywords.load(new File(opts.keywordsFile.value),opts.codec.value) else new KeywordsDB(Map())

    val mentions = LoadRexa.fromDir(new File(opts.rexaDir.value), opts.codec.value)
    val authorMentions = GenerateAuthorMentionsFromRexa.processAll(mentions,topicsDB,keywordsDB).toIndexedSeq
    val keystore = InMemoryKeystore.fromFile(new File(opts.keystorePath.value),opts.keystoreDim.value,opts.keystoreDelim.value,opts.codec.value)

    val canopyFunctions = Iterable((a:AuthorMention) => a.canopy.value)
    
    val experiment = new ExperimentWithGroundTruth[AuthorMention] {
      override val algorithm: CoreferenceAlgorithm[AuthorMention] with IndexableMentions[AuthorMention] = new HierarchicalCoreferenceAlgorithm(opts,authorMentions,keystore,canopyFunctions)

      override def goldClustering: Iterable[(String, String)] = mentions.map(f => (f.id,f.groundTruth.get))
    }
    
    new File(opts.outputDir.value).mkdirs()
    experiment.performExperiment(new File(opts.outputDir.value),writeHTML = false, opts.numThreads.value)
    println(experiment.results)
    
    
  }
  
}

object RunRexaDeterministicBaseline {


  def main(args: Array[String]): Unit = {

    val opts = new RunRexaOpts
    opts.parse(args)

    println("Loading the topics database")
    val topicsDB = if (opts.topicsFile.wasInvoked) LoadTopics.load(new File(opts.topicsFile.value),opts.codec.value) else new TopicsDB(Map())
    val keywordsDB = if (opts.keywordsFile.wasInvoked) LoadKeywords.load(new File(opts.keywordsFile.value),opts.codec.value) else new KeywordsDB(Map())

    val mentions = LoadRexa.fromDir(new File(opts.rexaDir.value), opts.codec.value)
    val authorMentions = GenerateAuthorMentionsFromRexa.processAll(mentions,topicsDB,keywordsDB)

    val canopyFunction = (a:AuthorMention) => a.canopy.value

    val experiment = new ExperimentWithGroundTruth[AuthorMention] {
      override val algorithm: CoreferenceAlgorithm[AuthorMention] with IndexableMentions[AuthorMention] = new DeterministicBaselineCoreferenceAlgorithm(authorMentions,canopyFunction)

      override def goldClustering: Iterable[(String, String)] = mentions.map(f => (f.id,f.groundTruth.get))
    }

    new File(opts.outputDir.value).mkdirs()
    experiment.performExperiment(new File(opts.outputDir.value),writeHTML = false, opts.numThreads.value)
    println(experiment.results)


  }

}