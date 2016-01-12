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

import edu.umass.cs.iesl.author_coref.data_structures.Author

object Canopies {

  def fullName = (author: Author) => "LAST_" + author.lastName.opt.getOrElse("") + "_FIRST_" + author.firstName.opt.getOrElse("") + "_MIDDLE_" + author.middleNames.opt.getOrElse(Seq()).mkString("_")

  def firstAndLast = (author: Author) => "LAST_" + author.lastName.opt.getOrElse("") + "_FIRST_" + author.firstName.opt.getOrElse("")

  def lastAndFirstNofFirst = (author: Author, n: Int) =>"LAST_" + author.lastName.opt.getOrElse("") + "_FIRST_" + author.firstName.opt.getOrElse("").take(n)

  private val lastAndFirstNPattern = "(lastandfirst[0-9]+offirst)".r
  def fromString(canopyName: String) =
    canopyName.toLowerCase match {
    case "fullname" =>
      fullName
    case "firstandlast" =>
      firstAndLast
    case lastAndFirstNPattern(canopy) =>
        val num = canopy.replaceAll("[^0-9]","").toInt
      (author: Author) => lastAndFirstNofFirst(author,num)
  }

}
