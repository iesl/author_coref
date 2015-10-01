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

/**
 * Helper class for loading Rexa. Uses regexes to match the XML like Rexa formatting.
 * @param tagName - the name of the tag
 */
case class AlmostXMLTag(tagName: String) {

  /**
   * The regex that is used to find the value(s) for the tag.
   */
  val regex = s"<$tagName>(?:(?!<\\/$tagName>).)*<\\/$tagName>".r

  private val replacementRegex = s"<[\\/]*$tagName>"

  /**
   * Returns all of the values associated with this tag.
   * @param almostXml - the string to extract values from
   * @return - the values associated with the tag
   */
  def getValues(almostXml: String): Iterable[String] = {
    regex.findAllIn(almostXml).map(_.replaceAll(replacementRegex, "").trim).toIterable
  }

  /**
   * Returns the first value associated with this tag.
   * @param almostXML - the string to extract values from
   * @return - the values associated with the tag.
   */
  def getFirstValue(almostXML: String): Option[String] = {
    regex.findFirstIn(almostXML).map(_.replaceAll(replacementRegex, "").trim)
  }

}
