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

import edu.umass.cs.iesl.author_coref.data_structures.coreference.CorefMention

class ACLAuthorMention extends CorefMention {

  val paperId = StringSlot("paperId")
  val self = CubbieSlot[Author]("self", () => new Author())
  val title = StringSlot("title")
  val venues = CubbieListSlot[Venue]("venues", () => new Venue())
  val year = StringSlot("year")
  val coauthors = CubbieListSlot[Author]("coauthors", () => new Author())
  val abstractText = StringSlot("abstractText")


  def this(mentionId: String, paperId: String, self: Author, title: Option[String], venues: Option[Seq[Venue]], year: Option[String], coauthors: Option[Seq[Author]], abstractText: Option[String]) = {
    this()
    this.mentionId.set(mentionId)
    this.paperId.set(paperId)
    this.self.set(self)
    this.title.set(title)
    this.venues.set(venues)
    this.year.set(year)
    this.coauthors.set(coauthors)
    this.abstractText.set(abstractText)
  }
}



class ACLPaper extends CubbieWithHTMLFormatting {
  
  val paperId = StringSlot("paperId")
  val title = StringSlot("title")
  val venues = CubbieListSlot[Venue]("venues", () => new Venue())
  val year = StringSlot("year")
  val authors = CubbieListSlot[Author]("authors", () => new Author())
  val abstractText = StringSlot("abstractText")
  
  def this(paperId: String, title: Option[String], venues: Option[Seq[Venue]], year: Option[String], authors: Option[Seq[Author]], abstractText: Option[String]) = {
    this()
    this.paperId.set(paperId)
    this.title.set(title)
    this.venues.set(venues)
    this.year.set(year)
    this.authors.set(authors.map(_.filterNot(a=> a.lastName.opt.isEmpty || a.lastName.value.isEmpty)))
    this.abstractText.set(abstractText)
  }
  
  def toACLAuthorMentions = {
    authors.opt.map(authors =>
      authors.map(self => {
        val coauthors = authors.filterNot(_ == self)
        val mentionId = (paperId.value + "_LN_" + self.lastName.opt.getOrElse("") + "_FN_" + self.firstName.opt.getOrElse("")).replaceAll("[^a-zA-Z0-9\\-_]","")
        new ACLAuthorMention(mentionId,paperId.value,self,title.opt,venues.opt,year.opt,Some(coauthors),abstractText.opt)
      })
    ).getOrElse(Iterable())
  }
  
}
