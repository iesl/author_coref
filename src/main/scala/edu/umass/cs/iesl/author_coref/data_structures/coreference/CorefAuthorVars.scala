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

import cc.factorie.app.nlp.hcoref.{GroundTruth, NodeVariables}
import cc.factorie.variable.{BagOfWordsVariable, DenseDoubleBagVariable, DiffList, NoopDiff}
import edu.umass.cs.iesl.author_coref.coreference.MutableSingularCanopy

// Modified from AuthorVars in factorie
class CorefAuthorVars(
                      // Author information
                      val firstNames:BagOfWordsVariable,
                      val middleNames:BagOfWordsVariable,
                      val lastNames: BagOfWordsVariable,
                      val emails: BagOfWordsVariable,
                      val institutions: BagOfWordsVariable,
                      val coauthors: BagOfWordsVariable,

                      // Publication information
                      val titleEmbeddingKeywords: DenseDoubleBagVariable, // Embedding of the title
                      val discreteTopics: BagOfWordsVariable, // Topics from LDA
                      val text: BagOfWordsVariable, // Sparse BoW of the text
                      val textEmbeddingKeywords: DenseDoubleBagVariable, // Embedding of the text
                      val keywords: BagOfWordsVariable, // keywords, both specified and derived
                      val venues: BagOfWordsVariable, // the venue information
                      
                      // Bookkeeping
                      var canopy:String,
                      val truth:BagOfWordsVariable, source:String = "") extends NodeVariables[CorefAuthorVars] with MutableSingularCanopy with GroundTruth {

  
  def this(dim: Int) = this(new BagOfWordsVariable(),new BagOfWordsVariable(),new BagOfWordsVariable(),new BagOfWordsVariable(), new BagOfWordsVariable(), new BagOfWordsVariable(), new DenseDoubleBagVariable(dim), new BagOfWordsVariable(), new BagOfWordsVariable(), new DenseDoubleBagVariable(), new BagOfWordsVariable(), new BagOfWordsVariable(), "",new BagOfWordsVariable(), "")
  def this() = this(200)

  var provenance: Option[AuthorMention] = None

  def getVariables = Seq(firstNames, middleNames, lastNames, emails, institutions, coauthors, titleEmbeddingKeywords, discreteTopics, text, textEmbeddingKeywords,keywords, venues)
  
  def --=(other: CorefAuthorVars)(implicit d: DiffList) {
    this.firstNames remove other.firstNames.value
    this.middleNames remove other.middleNames.value
    this.lastNames remove other.lastNames.value
    this.emails remove other.emails.value
    this.institutions remove other.institutions.value
    this.coauthors remove other.coauthors.value
    this.titleEmbeddingKeywords remove other.titleEmbeddingKeywords.value
    this.discreteTopics remove other.discreteTopics.value
    this.text remove other.text.value
    this.textEmbeddingKeywords remove other.textEmbeddingKeywords.value
    this.keywords remove other.keywords.value
    this.venues remove other.venues.value
    this.truth remove other.truth.value
    if (d ne null) d += NoopDiff(this) // because EntityNameTemplate (and others) have InventorVar as its neighbor, but doesn't have the bags of words as neighbors
  }

  def ++=(other: CorefAuthorVars)(implicit d: DiffList) {
    this.firstNames add other.firstNames.value
    this.middleNames add other.middleNames.value
    this.lastNames add other.lastNames.value
    this.emails add other.emails.value
    this.institutions add other.institutions.value
    this.coauthors add other.coauthors.value
    this.titleEmbeddingKeywords add other.titleEmbeddingKeywords.value
    this.discreteTopics add other.discreteTopics.value
    this.text add other.text.value
    this.textEmbeddingKeywords add other.textEmbeddingKeywords.value
    this.keywords add other.keywords.value
    this.venues add other.venues.value
    this.truth add other.truth.value
    if (d ne null) d += NoopDiff(this) // because EntityNameTemplate (and others) have InventorVar as its neighbor, but doesn't have the bags of words as neighbors
  }

  def --(other: CorefAuthorVars)(implicit d: DiffList) = new CorefAuthorVars(
    firstNames = this.firstNames -- other.firstNames,
    middleNames = this.middleNames -- other.middleNames,
    lastNames = this.lastNames -- other.lastNames,
    emails = this.emails -- other.emails,
    institutions = this.institutions -- other.institutions,
    coauthors = this.coauthors -- other.coauthors,
    titleEmbeddingKeywords = this.titleEmbeddingKeywords -- other.titleEmbeddingKeywords,
    discreteTopics = this.discreteTopics -- other.discreteTopics,
    text = this.text -- other.text,
    textEmbeddingKeywords = this.textEmbeddingKeywords -- other.textEmbeddingKeywords,
    keywords = this.keywords -- other.keywords,
    venues = this.venues -- other.venues,
    canopy = this.canopy,
    truth = this.truth -- other.truth)
  
  def ++(other: CorefAuthorVars)(implicit d: DiffList) = new CorefAuthorVars(
    firstNames = this.firstNames ++ other.firstNames,
    middleNames = this.middleNames ++ other.middleNames,
    lastNames = this.lastNames ++ other.lastNames,
    emails = this.emails ++ other.emails,
    institutions = this.institutions ++ other.institutions,
    coauthors = this.coauthors ++ other.coauthors,
    titleEmbeddingKeywords = this.titleEmbeddingKeywords ++ other.titleEmbeddingKeywords,
    discreteTopics = this.discreteTopics ++ other.discreteTopics,
    text = this.text ++ other.text,
    textEmbeddingKeywords = this.textEmbeddingKeywords ++ other.textEmbeddingKeywords,
    keywords = this.keywords ++ other.keywords,
    venues = this.venues ++ other.venues,
    canopy = this.canopy,
    truth = this.truth ++ other.truth)

}