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

import cc.factorie.app.bib.parser.Dom
import org.apache.commons.lang3.StringUtils._


class Author extends PersonName {
  
  val emails = new StringListSlot("emails")
  val institutions = new StringListSlot("institutions")

  def this(first: String, middles: Seq[String], last: String) = {
    this()
    firstName.set(first)
    middleNames.set(middles)
    lastName.set(last)
  }

  def this(first: Option[String], middles: Seq[String], last: Option[String]) = {
    this()
    firstName.set(first)
    middleNames.set(middles)
    lastName.set(last)
  }

  lazy val middleInitials = middleNames.opt.map(f => f.map(_.head.toString)).getOrElse(Iterable())

  lazy val normalized: Author = new Author(firstName.opt.map(AuthorName.normalize),middleNames.opt.getOrElse(Seq()).map(AuthorName.normalize),lastName.opt.map(AuthorName.normalize))

  def formattedString = lastName.opt.mkString("","",", ") + (firstName.opt ++ middleNames.opt.getOrElse(Iterable())).mkString(" ")

  def isEmpty: Boolean =
    (firstName.opt.isEmpty || firstName.value == "") && (middleNames.opt.isEmpty || (middleNames.opt.isDefined && middleNames.value.isEmpty))  && (lastName.opt.isEmpty || lastName.value == "")

  def equals(other: Author): Boolean = {
    val sameFirst = if (firstName.isDefined && other.firstName.isDefined) 
      firstName.value == other.firstName.value 
    else if (!firstName.isDefined && !other.firstName.isDefined)
      true
    else 
      false

    val sameMiddles = if (middleNames.isDefined && other.middleNames.isDefined)
      middleNames.value.toSet equals  other.middleNames.value.toSet
    else if (!middleNames.isDefined && !other.middleNames.isDefined)
      true
    else
      false
    
    val sameLast = if (lastName.isDefined && other.lastName.isDefined)
      lastName.value == other.lastName.value
    else if (!lastName.isDefined && !other.lastName.isDefined)
      true
    else
      false

    sameFirst && sameMiddles && sameLast
  }
  
}

/**
 * Methods for handling author names
 */
object AuthorName {

  /**
   * Normalizes an author name component. 
   * @param nameString - the name to normalized
   * @return - normalized version
   */
  def normalize(nameString: String) = stripAccents(nameString.toLowerCase.trim).replaceAll("""\s+""", " ")

  def apply(domName: Dom.Name): Author = {
    val author = new Author(clean(domName.first),clean(domName.von).split(" "),clean(domName.last))
    author
  }

  def clean(name: String) = name.replaceAll("\t|\n", " ")

}
