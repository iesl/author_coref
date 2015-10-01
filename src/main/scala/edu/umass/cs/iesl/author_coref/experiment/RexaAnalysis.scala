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

package edu.umass.cs.iesl.author_coref.experiment

import java.io.File
import java.util
import edu.umass.cs.iesl.author_coref.data_structures.RexaAuthorMention
import edu.umass.cs.iesl.author_coref.load.LoadRexa

import scala.collection.mutable
import scala.collection.JavaConverters._

object RexaAnalysis {

  // Unique value -> num occ
  type AnalysisMap = mutable.Map[String,Clustering]
  
  // Map of entity id to mention id
  type Clustering = mutable.Map[String,mutable.Set[String]]
  
  // Unique value -> groups of coreference mentions with that value

  val editor = new util.HashMap[String,Clustering]().asScala
  val email = new util.HashMap[String,Clustering]().asScala
  val journal = new util.HashMap[String,Clustering]().asScala
  val institution = new util.HashMap[String,Clustering]().asScala
  val keyword = new util.HashMap[String,Clustering]().asScala
  val venue = new util.HashMap[String,Clustering]().asScala
  
  
  def updateMap(map: AnalysisMap, value: String, mentionID: String, entityID: String) = {
    if (!map.contains(value))
      map.put(value,new util.HashMap[String,mutable.Set[String]]().asScala)
    if (!map(value).contains(entityID))
      map(value).put(entityID, new util.HashSet[String]().asScala)
    map(value)(entityID).add(mentionID)
  }
  
  def processMention(mention: RexaAuthorMention) = {
    mention.editor.foreach(e => updateMap(editor,e,mention.id,mention.groundTruth.get))
    mention.emails.foreach(e => updateMap(email,e,mention.id,mention.groundTruth.get))
    mention.journal.foreach(j => updateMap(journal,j,mention.id,mention.groundTruth.get))
    mention.institutions.foreach(i => updateMap(institution,i,mention.id,mention.groundTruth.get))
    mention.keyword.foreach(k => updateMap(keyword,k,mention.id,mention.groundTruth.get))
    mention.venue.foreach(v => updateMap(venue,v,mention.id,mention.groundTruth.get))
  }
  
  def printSummary(map: AnalysisMap,name: String) = {
    val uniqueValues = map.keys.mkString(" | ")
    val numberOfCoreferentMentionsWhichShareAValue = map.values.map(c => c.values.count(_.size > 1)).sum
    println(s"Name: $name")
    println(s"UniqueValues $uniqueValues")
    println(s"Number of Coreferent Mentions which share a value: $numberOfCoreferentMentionsWhichShareAValue")
  }
  
  def main(args: Array[String]) = {
    val rexaDir = args(0)
    val mentions = LoadRexa.fromDir(new File(rexaDir), "UTF-8")
    mentions.foreach(processMention)
    printSummary(editor,"editor")
    printSummary(email,"email")
    printSummary(journal,"journal")
    printSummary(institution,"institution")
    printSummary(keyword,"keyword")
    printSummary(venue,"venue")
  }

}
