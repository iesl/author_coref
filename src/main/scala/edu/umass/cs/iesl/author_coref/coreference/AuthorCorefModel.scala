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

package edu.umass.cs.iesl.author_coref.coreference

import cc.factorie.app.nlp.hcoref._
import cc.factorie.util.DefaultCmdOptions
import edu.umass.cs.iesl.author_coref.data_structures.coreference.CorefAuthorVars
import edu.umass.cs.iesl.author_coref.utilities.KeystoreOpts


/**
  * The AuthorCorefModel defines a "TemplateModel" in factorie. The model determines which
  * templates are used in the hierarchical coreference model. Templates can be added
  * to the model using "+=".
  */
class AuthorCorefModel extends CorefModel[CorefAuthorVars] with DebuggableModel[CorefAuthorVars]

object AuthorCorefModel {

  /**
    * Generates a model with parameters based on the command line options.
    * @param opts - the command line options storing the model parameters
    * @return
    */
  def fromCmdOptions(opts:AuthorCorefModelOptions):AuthorCorefModel = {
    implicit val authorCorefModel = new AuthorCorefModel

    // Structural penalty
    if(opts.entitySizeWeight.value != 0.0)authorCorefModel += new EntitySizePrior(opts.entitySizeWeight.value,opts.entitySizeExponent.value)

    // First names
    if(opts.bagFirstWeight.value != 0.0)authorCorefModel += new SizeLimitingEntityNameTemplate(opts.bagFirstInitialWeight.value,opts.bagFirstNameWeight.value,opts.bagFirstWeight.value,opts.bagFirstSaturation.value,opts.bagFirstNoNamePenalty.value, getBag =  { b:CorefAuthorVars => b.firstNames},bagName = "first initial")
    if(opts.bagFirstNoNamePenalty.value != 0.0)authorCorefModel += new EmptyBagPenalty(opts.bagFirstNoNamePenalty.value, {b:CorefAuthorVars => b.firstNames}, "first names")

    // Middle names
    if(opts.bagMiddleWeight.value != 0.0)authorCorefModel += new SizeLimitingEntityNameTemplate(opts.bagMiddleInitialWeight.value,opts.bagMiddleNameWeight.value,opts.bagMiddleWeight.value,opts.bagMiddleSaturation.value,opts.bagMiddleNoNamePenalty.value, getBag = {b:CorefAuthorVars => b.middleNames}, bagName = "middle initial")
    if(opts.bagMiddleNoNamePenalty.value != 0.0)authorCorefModel += new EmptyBagPenalty(opts.bagMiddleNoNamePenalty.value, {b:CorefAuthorVars => b.middleNames}, "middle names")

    // Emails
    if(opts.bagEmailsWeight.value != 0.0)authorCorefModel += new ChildParentCosineDistance(opts.bagEmailsWeight.value,opts.bagEmailsShift.value, {b:CorefAuthorVars => b.emails}, "emails")
    if(opts.bagEmailsEntropy.value != 0.0)authorCorefModel += new BagOfWordsEntropy(opts.bagEmailsEntropy.value, {b:CorefAuthorVars => b.emails}, "emails")
    if(opts.bagEmailsPrior.value != 0.0)authorCorefModel += new BagOfWordsSizePrior(opts.bagEmailsPrior.value, {b:CorefAuthorVars => b.emails}, "emails")

    // Institutions
    if(opts.bagInstitutionsWeight.value != 0.0)authorCorefModel += new ChildParentCosineDistance(opts.bagInstitutionsWeight.value,opts.bagInstitutionsShift.value, {b:CorefAuthorVars => b.institutions}, "institutions")
    if(opts.bagInstitutionsEntropy.value != 0.0)authorCorefModel += new BagOfWordsEntropy(opts.bagInstitutionsEntropy.value, {b:CorefAuthorVars => b.institutions}, "institutions")
    if(opts.bagInstitutionsPrior.value != 0.0)authorCorefModel += new BagOfWordsSizePrior(opts.bagInstitutionsPrior.value, {b:CorefAuthorVars => b.institutions}, "institutions")

    // co-authors
    if(opts.bagCoAuthorWeight.value != 0.0)authorCorefModel += new ChildParentCosineDistance(opts.bagCoAuthorWeight.value,opts.bagCoAuthorShift.value, {b:CorefAuthorVars => b.coauthors}, "coauthors")
    if(opts.bagCoAuthorEntropy.value != 0.0)authorCorefModel += new BagOfWordsEntropy(opts.bagCoAuthorEntropy.value, {b:CorefAuthorVars => b.coauthors}, "coauthors")
    if(opts.bagCoAuthorPrior.value != 0.0)authorCorefModel += new BagOfWordsSizePrior(opts.bagCoAuthorPrior.value, {b:CorefAuthorVars => b.coauthors}, "coauthors")

    // Title embedding
    if(opts.bagTitleEmbeddingWeight.value != 0.0)authorCorefModel += new DenseCosineDistance(opts.bagTitleEmbeddingWeight.value,opts.bagTitleEmbeddingShift.value, {b:CorefAuthorVars => b.titleEmbeddingKeywords}, "title embedding")
    if(opts.bagTitleEmbeddingEntropy.value != 0.0)authorCorefModel += new DenseBagOfWordsEntropy(opts.bagTitleEmbeddingEntropy.value, {b:CorefAuthorVars => b.titleEmbeddingKeywords}, "title embedding")

    // Discrete Topics
    if(opts.bagTopicsWeight.value != 0.0)authorCorefModel += new ChildParentCosineDistance(opts.bagTopicsWeight.value,opts.bagTopicsShift.value, {b:CorefAuthorVars => b.discreteTopics}, "topics")
    if(opts.bagTopicsEntropy.value != 0.0)authorCorefModel += new BagOfWordsEntropy(opts.bagTopicsEntropy.value, {b:CorefAuthorVars => b.discreteTopics}, "topics")
    if(opts.bagTopicsPrior.value != 0.0)authorCorefModel += new BagOfWordsSizePrior(opts.bagTopicsPrior.value, {b:CorefAuthorVars => b.discreteTopics}, "topics")

    // Text
    if(opts.bagTextWeight.value != 0.0)authorCorefModel += new ChildParentCosineDistance(opts.bagTextWeight.value,opts.bagTopicsShift.value, {b:CorefAuthorVars => b.text}, "text")
    if(opts.bagTextEntropy.value != 0.0)authorCorefModel += new BagOfWordsEntropy(opts.bagTextEntropy.value, {b:CorefAuthorVars => b.text}, "text")
    if(opts.bagTextPrior.value != 0.0)authorCorefModel += new BagOfWordsSizePrior(opts.bagTextPrior.value, {b:CorefAuthorVars => b.text}, "text")

    // Text embedding (skip for now)

    // Keywords
    if(opts.bagKeywordsWeight.value != 0.0)authorCorefModel += new ChildParentCosineDistance(opts.bagKeywordsWeight.value,opts.bagKeywordsShift.value, {b:CorefAuthorVars => b.keywords}, "keywords")
    if(opts.bagKeywordsEntropy.value != 0.0)authorCorefModel += new BagOfWordsEntropy(opts.bagKeywordsEntropy.value, {b:CorefAuthorVars => b.keywords}, "keywords")
    if(opts.bagKeywordsPrior.value != 0.0)authorCorefModel += new BagOfWordsSizePrior(opts.bagKeywordsPrior.value, {b:CorefAuthorVars => b.keywords}, "keywords")

    // Venues
    if(opts.bagVenuesWeight.value != 0.0)authorCorefModel += new ChildParentCosineDistance(opts.bagVenuesWeight.value,opts.bagVenuesShift.value, {b:CorefAuthorVars => b.venues}, "venues")
    if(opts.bagVenuesEntropy.value != 0.0)authorCorefModel += new BagOfWordsEntropy(opts.bagVenuesEntropy.value, {b:CorefAuthorVars => b.venues}, "venues")
    if(opts.bagVenuesPrior.value != 0.0)authorCorefModel += new BagOfWordsSizePrior(opts.bagVenuesPrior.value, {b:CorefAuthorVars => b.venues}, "venues")

    authorCorefModel
  }
}


/**
  * The command line options for the author coreference model used to determine the weights of the
  * templates in the model.
  */
trait AuthorCorefModelOptions extends DefaultCmdOptions with KeystoreOpts {

  // Author information

  // first names
  val bagFirstInitialWeight = new CmdOption("model-author-bag-first-initial-weight", 3.0, "N", "Penalty for first initial mismatches.")
  val bagFirstNoNamePenalty = new CmdOption("model-author-bag-first-noname-penalty", 2.0, "N", "Penalty for first initial mismatches.")
  val bagFirstNameWeight = new CmdOption("model-author-bag-first-name-weight", 3.0, "N", "Penalty for first name mismatches")
  val bagFirstSaturation = new CmdOption("model-author-bag-first-saturation", 16.0, "N", "Penalty for first initial mismatches.")
  val bagFirstWeight = new CmdOption("model-author-bag-first-weight", 1.0, "N", "Penalty for first initial mismatches.")

  // Middle names
  val bagMiddleInitialWeight = new CmdOption("model-author-bag-middle-initial-weight", 3.0, "N", "Penalty for first initial mismatches.")
  val bagMiddleNameWeight = new CmdOption("model-author-bag-middle-name-weight", 3.0, "N", "Penalty for first name mismatches")
  val bagMiddleSaturation = new CmdOption("model-author-bag-middle-saturation", 26.0, "N", "Penalty for first initial mismatches.")
  val bagMiddleWeight = new CmdOption("model-author-bag-middle-weight", 1.0, "N", "Penalty for first initial mismatches.")
  val bagMiddleNoNamePenalty = new CmdOption("model-author-bag-middle-noname-penalty", 0.25, "N", "Penalty for first initial mismatches.")

  // Emails
  val bagEmailsWeight = new CmdOption("model-author-bag-emails-weight", 10.0, "N", "Penalty for bag-of-emails template  (the author coreference model).")
  val bagEmailsShift = new CmdOption("model-author-bag-emails-shift", 0.0, "N", "Bag-of-emails shift for  (author coreference model).")
  val bagEmailsEntropy = new CmdOption("model-author-bag-emails-entropy", 0.0, "N", "Penalty on bag of emails entropy(author coreference model).")
  val bagEmailsPrior = new CmdOption("model-author-bag-emails-prior", 0.0, "N", "Bag of co-author prior penalty, formula is bag.size/bag.oneNorm*weight.")

  // Institutions
  val bagInstitutionsWeight = new CmdOption("model-author-bag-institutions-weight", 10.0, "N", "Penalty for bag-of-institutions template  (the author coreference  model).")
  val bagInstitutionsShift = new CmdOption("model-author-bag-institutions-shift", 0.0, "N", "Bag-of-institutions shift for  (author coreference model).")
  val bagInstitutionsEntropy = new CmdOption("model-author-bag-institutions-entropy", 0.0, "N", "Penalty on bag of institutions entropy(author coreference model).")
  val bagInstitutionsPrior = new CmdOption("model-author-bag-institutions-prior", 0.0, "N", "Bag of co-author prior penalty, formula is bag.size/bag.oneNorm*weight.")

  //co-authors
  val bagCoAuthorWeight = new CmdOption("model-author-bag-coauthors-weight", 4.0, "N", "Penalty for bag-of-co-authors cosine distance template (author coreference model).")
  val bagCoAuthorShift = new CmdOption("model-author-bag-coauthors-shift", -0.125, "N", "Shift for bag-of-co-authors cosine distance template  (author coreference model).")
  val bagCoAuthorEntropy = new CmdOption("model-author-bag-coauthors-entropy", 0.125, "N", "Penalty on bag-of-co-author entropy (author coreference model).")
  val bagCoAuthorPrior = new CmdOption("model-author-bag-coauthors-prior", 0.25, "N", "Bag of co-author prior penalty, formula is bag.size/bag.oneNorm*weight.")

  // Publication information


  // Title Embedding
  val bagTitleEmbeddingWeight = new CmdOption("model-author-bag-title-embedding-weight", 4.0, "N", "Penalty for bag-of-title-embedding cosine distance template (author coreference model).")
  val bagTitleEmbeddingShift = new CmdOption("model-author-bag-title-embedding-shift", -0.25, "N", "Shift for bag-of-title-embedding cosine distance template (author coreference model).")
  val bagTitleEmbeddingEntropy = new CmdOption("model-author-bag-title-embedding-entropy", 0.75, "N", "Penalty on bag of title-embedding entropy  (author coreference model).")
  val bagTitleEmbeddingPrior = new CmdOption("model-author-bag-title-embedding-prior", 0.25, "N", "Bag of title-embedding prior penalty, formula is bag.size/bag.oneNorm*weight.")
  val entitySizeExponent = new CmdOption("model-author-size-prior-exponent", 1.2, "N", "Exponent k for rewarding entity size: w*|e|^k")
  val entitySizeWeight = new CmdOption("model-author-size-prior-weight", 0.05, "N", "Weight w for rewarding entity size: w*|e|^k.")


  // Discrete Topic Labels
  val bagTopicsWeight = new CmdOption("model-author-bag-topics-weight", 4.0, "N", "Penalty for bag-of-topics cosine distance template (the author coreference model).")
  val bagTopicsShift = new CmdOption("model-author-bag-topics-shift", -0.125, "N", "Shift for bag-of-topics cosine distance template (author coreference model).")
  val bagTopicsEntropy = new CmdOption("model-author-bag-topics-entropy", 0.125, "N", "Penalty on bag-of-topics entropy (author coreference model).")
  val bagTopicsPrior = new CmdOption("model-author-bag-topics-prior", 0.25, "N", "Bag of topics prior penalty, formula is bag.size/bag.oneNorm*weight.")

  // Text 
  val bagTextWeight = new CmdOption("model-author-bag-text-weight", 4.0, "N", "Penalty for bag-of-text cosine distance template (the author coreference model).")
  val bagTextShift = new CmdOption("model-author-bag-text-shift", -0.125, "N", "Shift for bag-of-text cosine distance template (author coreference model).")
  val bagTextEntropy = new CmdOption("model-author-bag-text-entropy", 0.125, "N", "Penalty on bag-of-text entropy (author coreference model).")
  val bagTextPrior = new CmdOption("model-author-bag-text-prior", 0.25, "N", "Bag of text prior penalty, formula is bag.size/bag.oneNorm*weight.")

  // Skip the Text Embedding keywords for now

  // keywords
  val bagKeywordsWeight = new CmdOption("model-author-bag-keywords-weight", 2.0, "N", "Penalty for bag-of-keywords template  (the author coreference model).")
  val bagKeywordsShift = new CmdOption("model-author-bag-keywords-shift", -0.125, "N", "Bag-of-keywords shift for  (author coreference model).")
  val bagKeywordsEntropy = new CmdOption("model-author-bag-keywords-entropy", 0.25, "N", "Penalty on bag of keywrods entropy(author coreference model).")
  val bagKeywordsPrior = new CmdOption("model-author-bag-keywords-prior", 0.25, "N", "Bag of co-author prior penalty, formula is bag.size/bag.oneNorm*weight.")

  // venues
  val bagVenuesWeight = new CmdOption("model-author-bag-venues-weight", 4.0, "N", "Penalty for bag-of-venues cosine distance template (the author coreference model).")
  val bagVenuesShift = new CmdOption("model-author-bag-venues-shift", -0.125, "N", "Shift for bag-of-venues cosine distance template (author coreference model).")
  val bagVenuesEntropy = new CmdOption("model-author-bag-venues-entropy", 0.125, "N", "Penalty on bag-of-venue entropy (author coreference model).")
  val bagVenuesPrior = new CmdOption("model-author-bag-venues-prior", 0.25, "N", "Bag of co-author prior penalty, formula is bag.size/bag.oneNorm*weight.")

  //structural priors
  val depthPenalty = new CmdOption("model-depth-penalty", 0.0, "N", "Penalty depth in the tree.")

  // inference
  val maxIterations = new CmdOption("max-iterations",300000.0,"DOUBLE", "Max number of iterations")
  val iterationsMultiplier = new CmdOption("iterations-multiplier", 30.0,"DOUBLE", "This * numMentions is suggested number of iterations.")

}