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

package edu.umass.cs.iesl.author_coref.db

import java.io.File

import edu.umass.cs.iesl.author_coref._
import edu.umass.cs.iesl.author_coref.data_structures.ACLAuthorMention
import edu.umass.cs.iesl.author_coref.data_structures.coreference.AuthorMention
import edu.umass.cs.iesl.author_coref.load.{LoadKeywords, LoadTopics, LoadACL}
import edu.umass.cs.iesl.author_coref.utilities.{MongoDBOpts, NumThreads, CodecCmdOption}

object GenerateAuthorMentionsFromACL {

  def processAll(mentions: Iterator[ACLAuthorMention], topicsDatastore: Datastore[String,String], keywordsDatastore: Datastore[String, String]) = mentions.map(processSingle(_,topicsDatastore,keywordsDatastore))

  def processSingle(mention: ACLAuthorMention, topicsDatastore: Datastore[String,String], keywordsDatastore: Datastore[String, String]) = {

    val authorMention = new AuthorMention()

    // Set the ID
    authorMention.mentionId.set(mention.mentionId.value)

    // Don't Set the canopy

    // Create the author in focus
    authorMention.self.set(mention.self.value.normalized)

    // Co authors
    authorMention.coauthors.set(mention.coauthors.opt.map(_.map(_.normalized)))

    // Title
    authorMention.title.set(mention.title.opt)

    // Title Embedding keywords
    val titleEmbeddingKeywords = mention.title.opt.getOrElse("").split("\\s+").toIterable.filter(_.nonEmpty).map(_.removePunctuation().toLowerCase)
    authorMention.titleEmbeddingKeywords.set(titleEmbeddingKeywords.toSeq)

    // Discrete Topics
    val topics = topicsDatastore.get(mention.mentionId.value)
    authorMention.topics.set(topics.toSeq)

    // Text
    val text = mention.abstractText.opt.getOrElse("").trim.noneIfEmpty
    authorMention.text.set(text)

    // Tokenized Text
    val tokenizedText = text.getOrElse("").toLowerCase.removePunctuation(" ").split(" ").filter(_.nonEmpty)
    authorMention.tokenizedText.set(tokenizedText)

    // Keywords
    val foundKeywords = keywordsDatastore.get(mention.mentionId.value)
    authorMention.keywords.set(foundKeywords.toSeq)

    authorMention
  }
}

class PopulateAuthorMentionDBWithACLMentionsOpts extends CodecCmdOption with NumThreads with MongoDBOpts {
  val aclDir = new CmdOption[String]("acl-dir", "The directory containing the Grobid formatted ACL files", true)
  val aclFileCodec = new CmdOption[String]("acl-file-codec", "UTF-8", "STRING", "The file encoding of the ACL files. Note that the normal codec cmd line option will be used for all other files.")
  val bufferSize = new CmdOption[Int]("buffered-size", "The size of the buffer to use", true)
  val topicsFile = new CmdOption[String]("topics-file", "The file containing the topics for each paper", false)
  val keywordsFile = new CmdOption[String]("keywords-file", "The file containing the keywords for each paper", false)
}


object PopulateAuthorMentionDBWithACLMentions {
  
  def main(args: Array[String]): Unit = {
    val opts = new PopulateAuthorMentionDBWithACLMentionsOpts
    opts.parse(args)
    
    // Create the connection to Mongo
    println("Setting up Mongo database object")
    val db = new AuthorMentionDB(opts.hostname.value,opts.port.value,opts.dbname.value,opts.collectionName.value,false)
    
    // Load the Topic and Keyword files
    val topicsDB = if (opts.topicsFile.wasInvoked && opts.topicsFile.value.nonEmpty) {
      println("Loading Topics File")
      LoadTopics.load(new File(opts.topicsFile.value),opts.codec.value)
    } else 
      new EmptyDataStore[String,String]()

    val keywordsDB = if (opts.keywordsFile.wasInvoked && opts.keywordsFile.value.nonEmpty) {
      println("Loading Keywords File")
      LoadKeywords.load(new File(opts.keywordsFile.value),opts.codec.value)
    } else
      new EmptyDataStore[String,String]()
    
    // Load the papers
    val paperStreams = LoadACL.fromDir(new File(opts.aclDir.value),opts.numThreads.value,printMessages = false, codec = opts.aclFileCodec.value)
    // Expand into ACL Mentions
    val aclmentionStreams = paperStreams.map(_.flatMap(f => f.toACLAuthorMentions))
    // Convert into AuthorMentions
    val authorMentionStreams = aclmentionStreams.map(GenerateAuthorMentionsFromACL.processAll(_,topicsDB,keywordsDB))
    
    PopulateAuthorMentionDB.insertPar(authorMentionStreams,db, opts.bufferSize.value)
    db.addIndices()
  }
  
  
}