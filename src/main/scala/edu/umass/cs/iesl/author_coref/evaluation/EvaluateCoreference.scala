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

package edu.umass.cs.iesl.author_coref.evaluation

import edu.umass.cs.iesl.author_coref.data_structures.HTMLFormatting
import edu.umass.cs.iesl.author_coref.data_structures.coreference.{AuthorMention, CorefMention}

import scala.collection.mutable.ArrayBuffer


class EvaluateCoreference[P <: Comparable[P],C](algName: String, predicted: Iterable[(P,C)], gold: Iterable[(P,C)]) {

  var truePositives: Set[(P,P)] = null
  var falseNegatives: Set[(P,P)] = null
  var falsePositives: Set[(P,P)] = null

  var recall = 0.0
  var splitting = 0.0
  var lumping = 0.0
  var precision = 0.0
  var f1 = 0.0

  def evaluate = {

    // 1. Check that they are evaluated on same set of points

    val predPoints = predicted.map(_._1)
    val goldPoints = gold.map(_._1)

    val predPointsUniq = predPoints.toSet[P]
    val goldPointsUniq = goldPoints.toSet[P]

    assert(predPointsUniq == goldPointsUniq, s"The two clustering results must be defined on the same set of points. There are ${predPointsUniq.size} predicted points and ${goldPointsUniq.size} gold points. There are ${predPointsUniq.diff(goldPointsUniq).size} points in the predicted set that do not appear in the gold set. There are ${goldPointsUniq.diff(predPointsUniq).size} points in the gold set that do not appear in the predicted set.")
    assert(predPoints.size == predPointsUniq.size, "The predicted clustering contains at least one point multiple times.")
    assert(goldPoints.size == goldPointsUniq.size, "The gold clustering contains at least one point multiple times.")

    // 2. Build pairwise coreference maps for each

    val predPairs = collectCoreferencePairs(predicted)
    val goldPairs = collectCoreferencePairs(gold)

    // 3. Use maps to evaluate coreference as described by manual

    truePositives = predPairs.intersect(goldPairs)
    falseNegatives = goldPairs.diff(predPairs)
    falsePositives = predPairs.diff(goldPairs)

    // 4. Package up the results:
    recall = truePositives.size.toDouble / (truePositives.size + falseNegatives.size).toDouble
    splitting = falseNegatives.size.toDouble / (truePositives.size + falseNegatives.size).toDouble
    lumping = falsePositives.size.toDouble / (truePositives.size + falseNegatives.size).toDouble
    precision = truePositives.size.toDouble / (truePositives.size + falsePositives.size).toDouble
    f1 = 2.0 * (recall * precision) / (recall + precision)

    val numEntities = predicted.map(_._2).toSet.size
    val numGoldEntities = gold.map(_._2).toSet.size


    val mdString = Iterable(algName,numEntities,precision,recall,f1,splitting,lumping,truePositives.size,falsePositives.size,falseNegatives.size).mkString("| ", " | ", " |")
    Map("Num. Pred Entities" -> numEntities, "Num. Gold Entities" -> numGoldEntities, "recall" -> recall, "splitting" -> splitting, "lumping" -> lumping, "precision" -> precision, "f1" -> f1, "Num. True Positives" -> truePositives.size, "Num. False Negatives" -> falseNegatives.size, "Num. False Positives" -> falsePositives.size, "mdString" -> mdString)
  }

  def formattedString(map: Map[String, Any]) = (Iterable("Clustering Results:\n") ++ map.map(f => f._1 + ": " + f._2)).mkString("\n")

  def collectCoreferencePairs(clustering: Iterable[(P,C)]) = {
    val byClusterID = clustering.groupBy(_._2).mapValues(_.map(_._1).toIndexedSeq.sorted)
    val coreferencePairs = new ArrayBuffer[(P,P)](byClusterID.map(ps => ps._2.size * ps._2.size).sum)
    byClusterID.foreach{
      case (clusterID, pointsInCluster) =>
        var i = 0
        while (i < pointsInCluster.length-1) {
          var j = i + 1
          while (j < pointsInCluster.length) {
            coreferencePairs.+=((pointsInCluster(i),pointsInCluster(j)))
            j += 1
          }
          i += 1
        }
    }
    coreferencePairs.toSet
  }

  def clusterHTMLReport(clustering: Iterable[Iterable[P]], lookup: P => CorefMention with HTMLFormatting, title: String) = {
    val sb = new StringBuilder
    sb.append(s"<html>\n<body>\n")
    sb.append(s"\n<h1>$title</h1>")
    clustering.zipWithIndex.foreach{
      case (cluster,idx) =>
        sb.append(s"\n<h2>Cluster #$idx</h2>")
        cluster.foreach(f => sb.append(s"\n<br>${lookup(f).toHTMLFormattedString}<br>\n"))
    }
    sb.append("</body></html>")
  }
  
  def pairwiseHTMLReport(pairs: Iterable[(P,P)], lookup: P => CorefMention with HTMLFormatting, title: String) = {

    val sb = new StringBuilder
    sb.append(s"<html>\n<body>\n")
    sb.append(s"\n<h1>$title</h1>")
    pairs.zipWithIndex.foreach{
      case (pair,idx) =>
        sb.append(s"\n<h3>Pair #$idx</h3>")
        sb.append(s"\n<br>${lookup(pair._1).toHTMLFormattedString}<br><br>\n")
        sb.append(s"\n<br>${lookup(pair._2).toHTMLFormattedString}<br><br>\n")
    }
    sb.append("</body></html>")
  }

  def truePositivesReport(lookup: P => CorefMention with HTMLFormatting) =
    pairwiseHTMLReport(truePositives,lookup,"True Positives")

  def falsePositivesReport(lookup: P => CorefMention with HTMLFormatting) =
    pairwiseHTMLReport(falsePositives,lookup,"False Positives")

  def falseNegativesReport(lookup: P => CorefMention with HTMLFormatting) =
    pairwiseHTMLReport(falseNegatives,lookup,"False Negatives")

  def errorAnalysis(lookup: P => CorefMention with HTMLFormatting) = {

    // Number of FPs where inventors have the same first and last name spellings (ignoring the middle)
    val FP_sameFirstAndLastNameSpelling = falsePositives.count{
      pair =>
        val m1 = lookup(pair._1)
        val m2 = lookup(pair._2)
        (m1,m2) match {
          case ((inv1: AuthorMention, inv2: AuthorMention)) =>
          //  inv1.self.value.sameFirstAndLastName(inv2.self.value)
            false
          case _ =>
            false
        }
    }

    // Number of FNs where inventors have the same first and last name spellings (ignoring the middle)
    val FN_sameFirstAndLastNameSpelling = falseNegatives.count{
      pair =>
        val m1 = lookup(pair._1)
        val m2 = lookup(pair._2)
        (m1,m2) match {
          case ((inv1: AuthorMention, inv2: AuthorMention)) =>
            //inv1.self.value.sameFirstAndLastName(inv2.self.value)
            false
          case _ =>
            false
        }
    }

    // Number of FNs where inventors have the same first, middle and last name spellings
    val FN_sameFullNameSpelling = falseNegatives.count{
      pair =>
        val m1 = lookup(pair._1)
        val m2 = lookup(pair._2)
        (m1,m2) match {
          case ((inv1: AuthorMention, inv2: AuthorMention)) =>
            //inv1.self.value.sameFullName(inv2.self.value)
            false
          case _ =>
            false
        }
    }

    // Number of FNs where inventors have the same last name, but a differently spelled first name
    val FN_sameLastDiffFirst = falseNegatives.count{
      pair =>
        val m1 = lookup(pair._1)
        val m2 = lookup(pair._2)
        (m1,m2) match {
          case ((inv1: AuthorMention, inv2: AuthorMention)) =>
            //inv1.self.value.sameLastName(inv2.self.value) && !inv1.self.value.sameFirstName(inv2.self.value)
            false
          case _ =>
            false
        }
    }

    // Number of FNs where inventors have differently spelled last names
    val FN_differentLastName = falseNegatives.count{
      pair =>
        val m1 = lookup(pair._1)
        val m2 = lookup(pair._2)
        (m1,m2) match {
          case ((inv1: AuthorMention, inv2: AuthorMention)) =>
            //!inv1.self.value.sameLastName(inv2.self.value)
            false
          case _ =>
            false
        }
    }
    Map("False Positives: sameFirstAndLastNameSpelling" -> FP_sameFirstAndLastNameSpelling,
      "False Negatives: sameFirstAndLastNameSpelling" -> FN_sameFirstAndLastNameSpelling,
      "False Negatives: sameFullNameSpelling" -> FN_sameFullNameSpelling,
      "False Negatives: sameLastDiffFirst" -> FN_sameLastDiffFirst,
      "False Negatives: differentLastName" -> FN_differentLastName
    )
  }
}

