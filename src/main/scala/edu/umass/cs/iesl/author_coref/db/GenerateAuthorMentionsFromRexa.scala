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

import cc.factorie.app.nlp.lexicon.StopWords
import edu.umass.cs.iesl.author_coref._
import edu.umass.cs.iesl.author_coref.data_structures.coreference.AuthorMention
import edu.umass.cs.iesl.author_coref.data_structures.{Author, RexaAuthorMention}

import scala.collection.mutable.ArrayBuffer

// Creates the author mention from the rexa data structures
object GenerateAuthorMentionsFromRexa {

  def processAll(mentions: Iterable[RexaAuthorMention], topicsDatastore: Datastore[String,String], keywordsDatastore: Datastore[String, String]) = mentions.map(processSingle(_,topicsDatastore,keywordsDatastore))

  def processSingle(mention: RexaAuthorMention, topicsDatastore: Datastore[String,String], keywordsDatastore: Datastore[String, String]) = {

    val authorMention = new AuthorMention()

    // Set the ID 
    authorMention.mentionId.set(mention.id)
    authorMention.goldEntityId.set(mention.groundTruth)

    // Set the canopy
    authorMention.canopy.set(mention.canopy)
    authorMention.canopies.set(Seq(mention.canopy))

    // Create the author in focus
    val authorInFocus = mention.author_in_focus.getOrElse(new Author("", Seq(), "")).normalized
    // set the emails and institutions
    authorInFocus.emails.set(mention.emails.map(_.trimBegEnd()).filter(_.nonEmpty).toSeq)
    authorInFocus.institutions.set(mention.institutions.map(_.trimBegEnd()).filter(_.nonEmpty).toSeq)

    authorMention.self.set(authorInFocus)

    // Co authors
    val coauthors = (mention.authorlist ++ mention.alt_authorlist).map(_.normalized).filterNot(_.isEmpty).filterNot(authorInFocus.equals)
    val nonDuplicates = new ArrayBuffer[Author](coauthors.size)
    coauthors.foreach(c => if (!nonDuplicates.exists(c.equals)) nonDuplicates += c )
    authorMention.coauthors.set(nonDuplicates)

    // Title
    if (mention.title.isDefined)
      authorMention.title.set(mention.title)
    else
      authorMention.title.set(mention.altTitle)

    // Title Embedding keywords
    val titleEmbeddingKeywords = (if (mention.title.isDefined) mention.title else mention.altTitle).map(_.split("\\s+")).getOrElse(Array()).toIterable.filter(_.nonEmpty).map(_.removePunctuation().toLowerCase).filterNot(StopWords.contains)
    authorMention.titleEmbeddingKeywords.set(titleEmbeddingKeywords.toSeq)

    // Discrete Topics
    val topics = topicsDatastore.get(mention.id)
    authorMention.topics.set(topics.toSeq)

    // Text
    val text = mention.abstractText.getOrElse("").trim.noneIfEmpty //+ " " + mention.body.getOrElse("")
    authorMention.text.set(text)

    // Tokenized Text
    val tokenizedText = text.getOrElse("").toLowerCase.removePunctuation(" ").split(" ").filter(_.nonEmpty)
    authorMention.tokenizedText.set(tokenizedText)

    // Keywords
    val foundKeywords = keywordsDatastore.get(mention.id)
    authorMention.keywords.set((mention.keyword ++ foundKeywords).map(_.trimBegEnd()).filter(_.nonEmpty).toSeq)

    //val authorInFocus = author_in_focus.getOrElse(OldAuthor("",Seq(),""))
    if (authorInFocus.isEmpty) {
      println(s"[${this.getClass.getCanonicalName}] WARNING: Empty Author in focus for mention: ${mention.id}")
    }

    authorMention
  }


}
