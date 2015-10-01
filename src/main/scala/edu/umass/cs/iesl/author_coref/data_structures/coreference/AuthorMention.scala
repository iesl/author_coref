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
import cc.factorie.variable.{BagOfWordsVariable, DenseDoubleBagVariable}
import edu.umass.cs.iesl.author_coref.coreference.Keystore
import edu.umass.cs.iesl.author_coref.data_structures._

class AuthorMention extends CorefMention {
  
  val self = new CubbieSlot[Author]("self", () => new Author())
  val coauthors = new CubbieListSlot[Author]("coauthors",() => new Author())
  val title = new StringSlot("title")
  val titleEmbeddingKeywords = new StringListSlot("titleEmbeddingKeywords")
  val topics = new StringListSlot("topics")
  val text = new StringSlot("text")
  val tokenizedText = new StringListSlot("tokenizedText")
  val venues = new CubbieListSlot[Venue]("venues", () => new Venue())
  val keywords = new StringListSlot("keywords")
  
  val canopies = new StringListSlot("canopies")
  val canopy = new StringSlot("canopy")
  val source = new StringSlot("source")


  def coAuthorStrings: Iterable[String] = coauthors.opt.getOrElse(Iterable()).map(_.normalized.formattedString)

  def venueStrings = venues.opt.getOrElse(Iterable()).flatMap(_.name.opt)


  def toAuthorVars(keystore: Keystore): CorefAuthorVars = {
    // Author information
    val firstNames = new BagOfWordsVariable()
    val middleNames = new BagOfWordsVariable()
    val lastNames = new BagOfWordsVariable()
    val emailsBag = new BagOfWordsVariable()
    val institutionsBag = new BagOfWordsVariable()

    self.opt.foreach (a => {
      a.firstName.opt.foreach(f => firstNames += f)
      a.middleNames.opt.foreach(m => middleNames ++= m)
      a.lastName.opt.foreach(l => lastNames += l)
      a.emails.opt.foreach(e => emailsBag ++= e)
      a.institutions.opt.foreach(i => institutionsBag ++= i)
    })
    
    // Title embedding
    val titleEmbeddingKeywordsBag = new DenseDoubleBagVariable(keystore.dimensionality)
    val embedding = keystore.generateVector(titleEmbeddingKeywords.opt.getOrElse(Iterable()))
    titleEmbeddingKeywordsBag.set(embedding)(null)
    
    // Topics
    val topicsBag = new BagOfWordsVariable()
    topicsBag ++= topics.opt.getOrElse(Iterable())

    // Text 
    val tokenizedTextBag =  new BagOfWordsVariable()
    tokenizedTextBag ++= tokenizedText.opt.getOrElse(Iterable())

    // No text embedding for now
    val textEmbeddingKeywordsBag = new DenseDoubleBagVariable(keystore.dimensionality)

    // keywords
    val keywordsBag = new BagOfWordsVariable()
    keywordsBag ++= keywords.opt.getOrElse(Iterable())

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

}