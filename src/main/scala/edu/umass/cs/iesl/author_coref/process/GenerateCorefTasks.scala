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

package edu.umass.cs.iesl.author_coref.process

import java.io.{File, PrintWriter}
import java.util

import edu.umass.cs.iesl.author_coref.coreference.Canopies
import edu.umass.cs.iesl.author_coref.data_structures.coreference.{AuthorMention, CorefTask}
import edu.umass.cs.iesl.author_coref.db.{EmptyDataStore, GenerateAuthorMentionsFromACL}
import edu.umass.cs.iesl.author_coref.load.{LoadBibtex, LoadBibtexSingleRecordPerFile, LoadACL, LoadJSONAuthorMentions}
import edu.umass.cs.iesl.author_coref.utilities.{CanopyOpts, NameProcessorOpts, CodecCmdOption, NumThreads}

import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer
import scala.io.Source

object GenerateCorefTasks {
  
  def fromMultiple(mentionStreams: Iterable[Iterator[AuthorMention]], assignment: AuthorMention => String, ids: Set[String], nameProcessor: NameProcessor = CaseInsensitiveReEvaluatingNameProcessor) = {
    @volatile var maps = new ArrayBuffer[Map[String,Iterable[String]]]()
    mentionStreams.par.foreach{
      mentions =>
        val r = assignmentMap(canopyAssignments(mentions.filter(p => ids.isEmpty || ids.contains(p.mentionId.value)), assignment,nameProcessor))
        synchronized {maps += r}
    }
    mergeMaps(maps)
  }
  
  def writeToFile(map:Iterable[(String,Iterable[String])], file: File) = {
    val pw = new PrintWriter(file, "UTF-8")
    map.foreach{
      case (name,ids) =>
        pw.println(CorefTask(name,ids).toString)
        pw.flush()
    }
    pw.close()
  }

  def canopyAssignments(mentions: Iterator[AuthorMention], assignment: AuthorMention => String,nameProcessor: NameProcessor) = {
    mentions.map(f => {nameProcessor.process(f.self.value); (assignment(f),f.mentionId.value)})
  }

  def assignmentMap(assignments: Iterator[(String,String)]) =
    assignments.toIterable.groupBy(f => f._1).mapValues(_.map(_._2))

  def mergeMaps(maps: Iterable[Map[String,Iterable[String]]]): Iterable[(String,Iterable[String])] = {

    val finalMap = new util.HashMap[String, ArrayBuffer[String]](maps.map(_.size).sum).asScala
    maps.foreach(_.foreach{
      case (string,iter)  =>
        if (!finalMap.contains(string))
          finalMap.put(string, new ArrayBuffer[String]())
        finalMap(string) ++= iter
    })
    // Since this was generated in parallel, we need to somehow in force an ordering
    finalMap.mapValues(f => f.sorted).toIndexedSeq.sortBy(m => (-m._2.size,m._1))
  }

}

class GenerateCorefTasksOpts extends CodecCmdOption with NumThreads with NameProcessorOpts with CanopyOpts {
  val idRestrictionsFile = new CmdOption[String]("id-restrictions-file", "Invoke this command option to restrict the ids used to the ones in this file. One id per line", false)
  val outputFile = new CmdOption[String]("output-file", "Where to write the output", true)
}

class GenerateCorefTasksFromACLOpts extends GenerateCorefTasksOpts with CodecCmdOption with NumThreads {
  val aclDir = new CmdOption[String]("acl-dir", "The ACL Grobid directory", true)
  val aclFileCodec = new CmdOption[String]("acl-file-codec", "UTF-8", "STRING", "The encoding of the files in the input directory")
}


object GenerateCorefTasksFromACL {
  
  def main(args: Array[String]): Unit = {
    val opts = new GenerateCorefTasksFromACLOpts
    opts.parse(args)

    // Load the papers
    val paperStreams = LoadACL.fromDir(new File(opts.aclDir.value),opts.numThreads.value,printMessages = false, codec = opts.aclFileCodec.value)
    // Expand into ACL Mentions
    val aclmentionStreams = paperStreams.map(_.flatMap(f => f.toACLAuthorMentions))
    // Convert into AuthorMentions
    val authorMentionStreams = aclmentionStreams.map(GenerateAuthorMentionsFromACL.processAll(_,new EmptyDataStore[String,String],new EmptyDataStore[String,String]))
    
    val canopyAssignment = (a: AuthorMention) => Canopies.lastAndFirstNofFirst(a.self.value,3)

    val ids = if (opts.idRestrictionsFile.wasInvoked) Source.fromFile(opts.idRestrictionsFile.value,opts.codec.value).getLines().toIterable.toSet[String] else Set[String]()
    
    val tasks = GenerateCorefTasks.fromMultiple(authorMentionStreams,canopyAssignment,ids)
    GenerateCorefTasks.writeToFile(tasks,new File(opts.outputFile.value))
  }
  
}

class GenerateCorefTasksFromJSONOpts extends GenerateCorefTasksOpts with CodecCmdOption with NumThreads {
  val jsonFile = new CmdOption[String]("json-file", "The JSON file containing the mentions", true)
}

object GenerateCorefTasksFromJSON {
  def main(args: Array[String]): Unit = {
    val opts = new GenerateCorefTasksFromJSONOpts()
    opts.parse(args)
    val mentions = LoadJSONAuthorMentions.loadMultiple(new File(opts.jsonFile.value),opts.codec.value,opts.numThreads.value)
    val canopyAssignment = opts.canopies.value.map(Canopies.fromString).map(fn => (authorMention: AuthorMention) => fn(authorMention.self.value)).last
    val ids = if (opts.idRestrictionsFile.wasInvoked) Source.fromFile(opts.idRestrictionsFile.value,opts.codec.value).getLines().toIterable.toSet[String] else Set[String]()
    val nameProcessor = NameProcessor.fromString(opts.nameProcessor.value)
    val tasks = GenerateCorefTasks.fromMultiple(mentions,canopyAssignment,ids,nameProcessor)
    GenerateCorefTasks.writeToFile(tasks,new File(opts.outputFile.value))
  }
}

class GenerateCorefTasksFromBibtexOpts extends GenerateCorefTasksOpts with CodecCmdOption with NumThreads {
  val inputDir = new CmdOption[String]("input-dir", "The input directory containing the bibtex records", true)
  val oneMentionPerFile = new CmdOption[Boolean]("one-mention-per-file", "Supported Bibtex formats: 1) record per file 2) more than one record per file. For (1), the mention ids are the filenames, for (2) they are unique integers")
}

object GenerateCorefTasksFromBibtex {

  def main(args: Array[String]): Unit = {
    val opts = new GenerateCorefTasksFromBibtexOpts
    opts.parse(args)
    val canopyAssignment = opts.canopies.value.map(Canopies.fromString).map(fn => (authorMention: AuthorMention) => fn(authorMention.self.value)).last
    val loader = if (opts.oneMentionPerFile.value) new LoadBibtexSingleRecordPerFile else new LoadBibtex
    val filenames = new File(opts.inputDir.value).list().filterNot(_.startsWith("\\.")).map(new File(opts.inputDir.value,_).getAbsolutePath)
    val groupedFilenames = filenames.grouped(filenames.length / opts.numThreads.value).toIterable
    val groupedMentions = groupedFilenames.map(g => loader.fromFilenames(g.toIterator,opts.codec.value))
    //val mentions = loader.multipleIteratorsFromFilenames(filenames.toIterator,opts.codec.value)
    val ids = if (opts.idRestrictionsFile.wasInvoked) Source.fromFile(opts.idRestrictionsFile.value,opts.codec.value).getLines().toIterable.toSet[String] else Set[String]()
    val tasks = GenerateCorefTasks.fromMultiple(groupedMentions,canopyAssignment,ids)
    val nameProcessor = NameProcessor.fromString(opts.nameProcessor.value)
    GenerateCorefTasks.fromMultiple(groupedMentions,canopyAssignment,ids,nameProcessor)
  }
}