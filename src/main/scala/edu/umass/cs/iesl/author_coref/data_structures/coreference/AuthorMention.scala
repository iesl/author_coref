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

package edu.umass.cs.iesl.author_coref.data_structures.coreference

import cc.factorie.app.nlp.hcoref.Mention
import cc.factorie.app.nlp.lexicon.StopWords
import cc.factorie.variable.{BagOfWordsVariable, DenseDoubleBagVariable}
import edu.umass.cs.iesl.author_coref.coreference.Keystore
import edu.umass.cs.iesl.author_coref.data_structures._

/**
 * AuthorMentions are the data structure used by the coreference algorithms
 * These data structures store the information pertaining to an ambiguous
 * instance of an author record
 */
class AuthorMention extends CorefMention {

  /**
   * The information about the author we are try to dismabiguate
   */
  val self = new CubbieSlot[Author]("self", () => new Author())

  /**
   * The co-author information
   */
  val coauthors = new CubbieListSlot[Author]("coauthors",() => new Author())

  /**
   * The title of the author's paper
   */
  val title = new StringSlot("title")

  /**
   * The words from the title that will be used in the word embedding representation
   * of the title. Note these are case sensitive and so should match the casing of
   * your learned embeddings.
   */
  val titleEmbeddingKeywords = new StringListSlot("titleEmbeddingKeywords")

  /**
   * Topic indicators if LDA or other topic model has been used.
   */
  val topics = new StringListSlot("topics")

  /**
   * The text of the mention, typically the abstract or paper body.
   */
  val text = new StringSlot("text")

  /**
   * A tokenized version of the text used in sparse BOW features
   */
  val tokenizedText = new StringListSlot("tokenizedText")

  /**
   * The venue of publication
   */
  val venues = new CubbieListSlot[Venue]("venues", () => new Venue())

  /**
   * The keywords of the publication
   */
  val keywords = new StringListSlot("keywords")

  /**
   * The canopies assigned to the mention
   */
  val canopies = new StringListSlot("canopies")

  /**
   * The singular canopy assigned to the mention
   */
  val canopy = new StringSlot("canopy")

  /**
   * The source of the mention.
   */
  val source = new StringSlot("source")


  def coAuthorStrings: Iterable[String] = coauthors.opt.getOrElse(Iterable()).flatMap(_.lastName.opt).filter(_.nonEmpty)

  def venueStrings = venues.opt.getOrElse(Iterable()).flatMap(_.name.opt).filter(_.nonEmpty)


  def toAuthorVars(keystore: Keystore): CorefAuthorVars = {
    // Author information
    val firstNames = new BagOfWordsVariable()
    val middleNames = new BagOfWordsVariable()
    val lastNames = new BagOfWordsVariable()
    val emailsBag = new BagOfWordsVariable()
    val institutionsBag = new BagOfWordsVariable()

    self.opt.foreach (a => {
      a.firstName.opt.filter(_.nonEmpty).foreach(f => firstNames += f)
      a.middleNames.opt.foreach(m => middleNames ++= m.filter(_.nonEmpty))
      a.lastName.opt.filter(_.nonEmpty).foreach(l => lastNames += l)
      a.emails.opt.foreach(e => emailsBag ++= e.filter(_.nonEmpty))
      a.institutions.opt.foreach(i => institutionsBag ++= i.filter(_.nonEmpty))
    })
    
    // Title embedding
    val titleEmbeddingKeywordsBag = new DenseDoubleBagVariable(keystore.dimensionality)
    val embedding = keystore.generateAvgVector(titleEmbeddingKeywords.opt.getOrElse(Iterable()).filterNot(StopWords.contains))
    titleEmbeddingKeywordsBag.set(embedding)(null)
    
    // Topics
    val topicsBag = new BagOfWordsVariable()
    topicsBag ++= topics.opt.getOrElse(Iterable()).filter(_.nonEmpty)

    // Text 
    val tokenizedTextBag =  new BagOfWordsVariable()
    tokenizedTextBag ++= tokenizedText.opt.getOrElse(Iterable()).filter(_.nonEmpty)

    // No text embedding for now
    val textEmbeddingKeywordsBag = new DenseDoubleBagVariable(keystore.dimensionality)

    // keywords
    val keywordsBag = new BagOfWordsVariable()
    keywordsBag ++= keywords.opt.getOrElse(Iterable()).filter(_.nonEmpty)

    // venues
    val venuesBag = new BagOfWordsVariable()
    venuesBag ++= venueStrings

    val coAuthorsBag = new BagOfWordsVariable()
    coAuthorsBag ++= coAuthorStrings

    val v = new CorefAuthorVars(firstNames,middleNames,lastNames,emailsBag,institutionsBag,coAuthorsBag,titleEmbeddingKeywordsBag,topicsBag,tokenizedTextBag,textEmbeddingKeywordsBag,keywordsBag,venuesBag,canopy.opt.getOrElse(GENERIC_CANOPY),new BagOfWordsVariable(),source.opt.getOrElse(UNKNOWN_SOURCE))
    v.provenance = Some(this)
    v
  }
  
  def toMentionNode(keystore: Keystore) = new Mention[CorefAuthorVars](toAuthorVars(keystore),mentionId.value)(null)
  override def tsvString: String = s"${self.value.firstName.opt.getOrElse("")}\t${self.value.middleNames.opt.getOrElse(Seq()).filter(_.nonEmpty).mkString(",")}\t${self.value.lastName.opt.getOrElse("")}\t${coAuthorStrings.mkString(",")}\t${self.value.institutions.opt.getOrElse(Iterable()).filter(_.nonEmpty).mkString(",")}\t${title.opt.getOrElse("")}\t${keywords.opt.getOrElse(Iterable()).filter(_.nonEmpty).mkString(",")}"
}