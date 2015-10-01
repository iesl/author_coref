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

package edu.umass.cs.iesl.author_coref.utilities

import java.io.File
import java.util

import scala.collection.mutable.ArrayBuffer
import scala.collection.JavaConverters._
import edu.umass.cs.iesl.author_coref._


object FindAllRexaFields {

  
  def main(args: Array[String]) = {
    val dir = args(0)
    findAllDir(new File(dir))
    println("The following fields appear more than once in a single document: ")
    println(fieldsThatOccurMoreThanOnceInASingleDocument.groupBy(f => f).mapValues(_.size).mkString("\n"))
    println("Here is a map of the fields to the number of times they appear: ")
    println(allFields.mkString("\n"))
  }
  
  
  
  def findAllDir(baseDirectory: File) = baseDirectory.allFilesRecursively(f => !f.getName.startsWith(".")).foreach(f => getStats(findAllFile(f)))
  
  
  // Stats we want to keep
  
  val fieldsThatOccurMoreThanOnceInASingleDocument = new ArrayBuffer[String]()
  val allFields = new util.HashMap[String,Int](100).asScala
  
  def findAllFile(file: File) = findAll(file.getStringContents())

  /**
   * For a given string, find each of the fields and the number of times they appear
   * @param string
   * @return
   */
  def findAll(string: String) = {
    string.split("\n").flatMap(_.split(":|=").filter(_.trimBegEnd().nonEmpty).headOption).groupBy(f => f).mapValues(_.size)
  }
  
  def getStats(m: Map[String,Int]) = {
    m.foreach(f => allFields.put(f._1,allFields.getOrElse(f._1,0) + f._2))
    fieldsThatOccurMoreThanOnceInASingleDocument ++= m.filter(_._2 > 1).keys
  }
}
