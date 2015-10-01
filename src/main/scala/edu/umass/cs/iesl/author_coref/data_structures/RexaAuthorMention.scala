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

package edu.umass.cs.iesl.author_coref.data_structures

import cc.factorie.app.nlp.lexicon.StopWords
import edu.umass.cs.iesl.author_coref._

case class RawRexaAuthorMention(id: String,
                                canopy: String,
                                groundTruth: Option[String],
                                abstractText: Option[String],
                                alt_authorlist: Iterable[Author],
                                altTitle: Option[String],
                                author_in_focus: Option[Author],
                                author_in_focus_score: Option[Double],
                                authorlist: Iterable[Author],
                                body: Option[String],
                                editor: Iterable[String],
                                emails: Iterable[String],
                                institutions: Iterable[String],
                                venue: Option[String],
                                journal: Option[String],
                                keyword: Option[String],
                                title: Option[String],
                                year: Option[String]
                                 ) {
  
  
  def cleanInstitution(institutionString:  Iterable[String]) = 
    cleanField(institutionString,InstitutionStopWords)
  
  def cleanEditor(editorString: Iterable[String]) =
    cleanField(editorString,EditorStopWords)
  
  def cleanJournal(journalStrings:  Iterable[String]) = {
    journalStrings.flatMap(journalString => {
    val cleaned = journalString.toLowerCase.removeNewLinesAndTabs()
    val cleanedSplit = cleaned.split(",").map(_.removePunctuation())
    val splitOnYears = cleanedSplit.map(_.split("year:").head)
    val removeNumbers = splitOnYears.map(_.replaceAll("[0-9]+",""))
    val removeSingleCharacters = removeNumbers.filter(_.size > 1)
    val withoutStopWords = removeSingleCharacters.filterNot(StopWords.contains).filterNot(JournalStopWords.contains)
    withoutStopWords})
  }
  
  def cleanKeywords(keywordsString:  Iterable[String]) =
    cleanField(keywordsString,KeywordStopWords)
  
  def cleanField(strings: Iterable[String], stopwords: Set[String]) = {
    strings.flatMap(string => {
      val cleaned = string.toLowerCase.removeNewLinesAndTabs()
      val split = cleaned.split(",").map(_.removePunctuation())
      val withoutStopWords = split.filterNot(StopWords.contains).filterNot(stopwords.contains)
      withoutStopWords
    })
  }
  
  def cleanEmails(strings: Iterable[String]) = {
    strings.flatMap(string => {
      val splitEmails = string.toLowerCase.split(",")
    val cleaned = splitEmails.map(f => f.filter(e => EmailCharacters.contains(e.toString)))
    cleaned
    })
  }
  
  
  def toRexaAuthorMention: RexaAuthorMention = {
    RexaAuthorMention(id,canopy,groundTruth,
      abstractText,alt_authorlist,altTitle,
      author_in_focus,author_in_focus_score,
      authorlist,body,cleanEditor(editor),
      cleanEmails(emails),cleanInstitution(institutions),
    cleanJournal(venue.toIterable),cleanJournal(Iterable(journal.getOrElse(""))),
    cleanKeywords(Iterable(keyword.getOrElse(""))),title,year)
  }
}



case class RexaAuthorMention(id: String,
                        canopy: String,
                        groundTruth: Option[String],
                        abstractText: Option[String],
                        alt_authorlist: Iterable[Author],
                        altTitle: Option[String],
                        author_in_focus: Option[Author],
                        author_in_focus_score: Option[Double],
                        authorlist: Iterable[Author],
                        body: Option[String],
                        editor: Iterable[String],
                        emails: Iterable[String],
                        institutions: Iterable[String],
                        venue: Iterable[String],
                        journal: Iterable[String],
                        keyword: Iterable[String],
                        title: Option[String],
                        year: Option[String]
                         )



