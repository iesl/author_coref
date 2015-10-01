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

package edu.umass.cs.iesl.author_coref.utilities

import java.io.{PrintWriter, File}

import cc.factorie.util.Threading
import edu.umass.cs.iesl.author_coref.data_structures.coreference.AuthorMention
import edu.umass.cs.iesl.author_coref.db.{GenerateAuthorMentionsFromACL, EmptyDataStore, GenerateAuthorMentionsFromRexa}
import edu.umass.cs.iesl.author_coref.load.{LoadACL, LoadRexa}

import scala.collection.mutable.ArrayBuffer
import edu.umass.cs.iesl.author_coref._

object GenerateKeywords {
  
  def processMentions(mentions: Iterable[AuthorMention], keywords: Set[String], numThreads: Int) = {
    val lowercaseKeywords = keywords.map(_.toLowerCase)
    val textIterable = mentions.map(f => {
      val text = (f.title.opt.getOrElse("") + " " + f.text.opt.getOrElse("")).removePunctuation(" ").toLowerCase.trimBegEnd()
      (f.mentionId.value,text)
    })
    processMultiple(textIterable,lowercaseKeywords,numThreads)
  }
  
  
  def processMultiple(textIterable: Iterable[(String,String)], keywords: Set[String], numThreads: Int) = {
    val phraseLength = keywords.map(_.split("_").length).max
    @volatile var results = new ArrayBuffer[(String,Iterable[String])](textIterable.size)
    Threading.parForeach(textIterable, numThreads)(pair => {
      val key = pair._1
      val rawText = pair._2
      val split = rawText.split("\\s+")
      val kws = processSingle(split,keywords,phraseLength)
      results += ((key,kws))
    })
    results
  }
  
  def processSingle(text: IndexedSeq[String], keywords: Set[String], phraseLength: Int) = {
    val docsKeywords = new ArrayBuffer[String]()
    var i = 0
    while (i < text.length) {
      var j = i + 1
      while (j < i + phraseLength && j < text.length) {
        val candidate = text.slice(i,j).mkString("_")
        if (keywords contains candidate)
          docsKeywords += candidate
        j += 1
      }
      i += 1
    }
    docsKeywords
  }
  
  def writeOutput(results: Iterable[(String, Iterable[String])], file: File, codec: String = "UTF-8") = {
    val pw = new PrintWriter(file, codec)
    results.foreach {
      pair =>
        pw.println((Iterable(pair._1) ++ pair._2).mkString("\t"))
        pw.flush()
    }
    pw.close()
  }
  
}


class GenerateKeywordsRexaOpts extends CodecCmdOption with NumThreads {
  val rexaDir = new CmdOption[String]("rexa-dir", "The rexa directory", true)
  val outputFile = new CmdOption[String]("output-file", "The output file", true)
  val keywordsFile = new CmdOption[String]("keywords-file", "Keywords, one word per line", true)
}


object GenerateKeywordsRexa {
  def main(args: Array[String]): Unit = {
    val opts = new GenerateKeywordsRexaOpts
    opts.parse(args)
    val rexaMentions = LoadRexa.fromDir(new File(opts.rexaDir.value), opts.codec.value)
    val authorMentions = GenerateAuthorMentionsFromRexa.processAll(rexaMentions,new EmptyDataStore[String,String](),new EmptyDataStore[String,String]())
    val keywords = new File(opts.keywordsFile.value).lines(opts.codec.value).toSet
    val results = GenerateKeywords.processMentions(authorMentions,keywords,opts.numThreads.value)
    GenerateKeywords.writeOutput(results,new File(opts.outputFile.value),opts.codec.value)
  }
}


class GenerateKeywordsACLOpts extends CodecCmdOption with NumThreads {
  val aclDir = new CmdOption[String]("acl-dir", "The acl directory", true)
  val aclFileCodec = new CmdOption[String]("acl-file-codec", "UTF-8", "STRING", "The file encoding of the parsed ACL")
  val outputFile = new CmdOption[String]("output-file", "The output file", true)
  val keywordsFile = new CmdOption[String]("keywords-file", "Keywords, one word per line", true)
}


object GenerateKeywordsACL {
  def main(args: Array[String]): Unit = {
    val opts = new GenerateKeywordsACLOpts
    opts.parse(args)
    // Load the papers
    val paperStreams = LoadACL.fromDir(new File(opts.aclDir.value),opts.numThreads.value,printMessages = false, codec = opts.aclFileCodec.value)
    // Expand into ACL Mentions
    val aclmentionStreams = paperStreams.map(_.flatMap(f => f.toACLAuthorMentions))
    // Convert into AuthorMentions
    val authorMentions = aclmentionStreams.map(GenerateAuthorMentionsFromACL.processAll(_,new EmptyDataStore[String,String],new EmptyDataStore[String,String])).flatten
    val keywords = new File(opts.keywordsFile.value).lines(opts.codec.value).toSet
    val results = GenerateKeywords.processMentions(authorMentions,keywords,opts.numThreads.value)
    GenerateKeywords.writeOutput(results,new File(opts.outputFile.value),opts.codec.value)
  }
}
