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

package edu.umass.cs.iesl.author_coref.load

import java.io.File

import cc.factorie.app.bib.parser.Dom
import edu.umass.cs.iesl.author_coref._
import edu.umass.cs.iesl.author_coref.data_structures.coreference.AuthorMention
import edu.umass.cs.iesl.author_coref.data_structures.{AuthorName, Venue}

/**
 * Load data in Bibtex format. Multiple mentions are stored in each bibtex file
 */
class LoadBibtex {

  @volatile var id = 0
  @volatile var numberOfPapers = 0
  @volatile var numberOfMentions = 0
  @volatile var numberOfErrors = 0

  def mentionId(file: File, authorSeqNo: Int) = s"bibtex_mention_$id-$authorSeqNo"

  def fromFilenames(filenames: Iterator[String], codec: String) = {
    val res = filenames.flatMap(s => fromFile(new File(s),codec))
    res
  }

  def multipleIteratorsFromFilenames(filenames: Iterator[String], codec: String) = {
    val res = filenames.map(s => fromFile(new File(s),codec).toIterator).toIterable
    res
  }

  def statusReport = s"[LoadBibtex] Loaded $numberOfPapers papers containing $numberOfMentions mentions. There were $numberOfErrors files that could not be parsed"

  val encodings = IndexedSeq("UTF-8","iso-8859-1","us-ascii")

  def fromFile(file: File, codec: String) = {
  //  println(s"[LoadBibtex] Parsing ${file.getName}")
    val fileContents = file.getStringContents(codec)
    var x = Dom.stringToDom(fileContents,expandAbbreviations = true)

    // try various encodings
    var i = 0
    while (x.isLeft && i < encodings.length) {
      if (!encodings(i).equalsIgnoreCase(codec)) {
        x = Dom.stringToDom(file.getStringContents(encodings(i)),expandAbbreviations = true)
      }
      i += 1
    }

    // left is when the document could not be parsed.
    if (x.isLeft) {
   //   println(s"[LoadBibtex] Error processing $file")
      synchronized {
        numberOfErrors += 1
        print(s"\r$statusReport")
      }
      List()
    } else {
      // right is when the document could be parsed
      val right = x.right.get

      val res = right.entries.flatMap {
        entry =>

          // The Bibtex Document
          val doc = entry._2

          // increment the id for the mention
          synchronized {
            id += 1
            numberOfPapers += 1
            print(s"\r$statusReport")
          }

          // The title
          val title = doc.otherFields.getOrElse("title", "")
          // Tokenized title
          val tokenizedTitle = title.split(" ")
          // The Dom.names
          val authorNames = doc.authors.getOrElse(List()).filter(_.last != "others").toSeq
          // Author objects
          val authors = authorNames.map(AuthorName.apply)
          // Abstract
          val abstractText = doc.otherFields.getOrElse("abstract", "").replaceAll("\n|\t", " ").replaceAll("\\s\\s", " ")
          val tokenizedText = abstractText.split(" ")
          // venue / publisher
          val venues = (doc.otherFields.get("journal") ++ doc.otherFields.get("booktitle") ++ doc.otherFields.get("conference") ++ doc.otherFields.get("publisher")).map(new Venue(_)).toSeq
          // keywords
          val keywords = doc.otherFields.getOrElse("keywords", "").split(" ")
          // institution
          val institutions = (doc.otherFields.get("department") ++ doc.otherFields.get("school")).toSeq
          authors.foreach {
            a =>
              a.institutions.set(institutions)
          }

          val authorMentions = authors.zipWithIndex.map {
            case(authorInFocus,seq) =>
              val mention = new AuthorMention()
              mention.mentionId.set(mentionId(file,seq))
              mention.self.set(authorInFocus)
              mention.title.set(title)
              mention.titleEmbeddingKeywords.set(tokenizedTitle)
              mention.coauthors.set(authors.filterNot(_ eq authorInFocus))
              mention.text.set(abstractText)
              mention.tokenizedText.set(tokenizedText)
              mention.venues.set(venues)
              mention.keywords.set(keywords)
              mention
          }

          synchronized {
            numberOfMentions += authorMentions.length
          }

          authorMentions
      }
      res
    }
  }
}


/**
 * Load Bibtex data. Assume a single record per file. The filename is used for the id of the mention
 */
class LoadBibtexSingleRecordPerFile extends LoadBibtex {
  override def mentionId(file: File, authorSeqNo: Int) = s"${file.getNameWithoutExtension}-$authorSeqNo"
}