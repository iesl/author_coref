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

import cc.factorie.util.Cubbie
import edu.umass.cs.iesl.author_coref._

trait HTMLFormatting {

  def toHTMLFormattedString: String
}

trait CubbieWithHTMLFormatting extends Cubbie with HTMLFormatting {   
  /**
   * Creates an HTML formatted string * 
   * @return
   */
  def toHTMLFormattedString: String = {
    val sb = new StringBuilder(1000)
    sb.append("\n<ul style=\"list-style-type:none\"><b>")
    sb.append(s"${this.getClass.ordinaryName}</b><br>")
    sb.append(HTMLFormatMap(this._map))
    sb.append("\n</ul>")
    sb.toString()
  }

  /**
   * Helper method to generate the HTML strings
   * @param map
   * @return
   */
  protected def HTMLFormatMap(map: scala.collection.Map[String,Any]): String = {
    val sb = new StringBuilder(1000)
    sb.append(s"\n<ul>")
    map.foreach{
      case (key, value) =>
        value match {
          case innerMap: scala.collection.Map[String,Any] =>
            sb.append(s"\n<li><b>$key</b>:</li>")
            sb.append(HTMLFormatMap(innerMap))
          case iterable: Iterable[Any] => 
            iterable.foreach{_ match {
              case anotherMap: scala.collection.Map[String,Any] =>
                sb.append(s"\n<li><b>$key</b>:</li>")
                sb.append(HTMLFormatMap(anotherMap))
              case _ => {}
            }}
            sb.append(s"\n<li><b>$key</b>:")
            iterable.foreach{_ match {
              case anotherMap: scala.collection.Map[String,Any] => {}
              case a: Any => sb.append(s"$a | ")
            }}
          case _ =>
            sb.append(s"\n<li><b>$key</b>: ${value.toString}</li>")
        }
    }
    sb.append("\n</ul>")
    sb.toString()
  }
  
}