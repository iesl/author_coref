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

import java.io.{File, PrintWriter}

import cc.factorie.util.{BasicEvaluatableClustering, EvaluatableClustering}
import edu.umass.cs.iesl.author_coref.coreference.CoreferenceAlgorithm
import edu.umass.cs.iesl.author_coref.data_structures.HTMLFormatting
import edu.umass.cs.iesl.author_coref.data_structures.coreference.CorefMention
import edu.umass.cs.iesl.author_coref.evaluation.EvaluateCoreference

/**
 * Code to run and evaluate with a gold labeling
 * @tparam MentionType
 */
trait ExperimentWithGroundTruth[MentionType <: CorefMention with HTMLFormatting] {

  val algorithm: CoreferenceAlgorithm[MentionType] with IndexableMentions[MentionType]

  def goldClustering: Iterable[(String, String)]

  lazy val predictedClusteringOnGoldPoints = {
    val goldPointIDs = goldClustering.map(_._1).toSet; algorithm.clusterIds.filter(p => goldPointIDs.contains(p._1))
  }

  lazy val evaluation = new EvaluateCoreference(algorithm.name, predictedClusteringOnGoldPoints, goldClustering)

  lazy val results: String = {
    val precisionRecallF1 = evaluation.formattedString(evaluation.evaluate)
    val pairwiseB3MUC = EvaluatableClustering.evaluationString(new BasicEvaluatableClustering(predictedClusteringOnGoldPoints), new BasicEvaluatableClustering(goldClustering))
    val mdStringOrig = (Iterable(algorithm.name) ++ pairwiseB3MUC.split("\n").drop(2).map(_.split(" ")).map(_.drop(1).mkString(" | "))).mkString("| ", " | ", " |")
    pairwiseB3MUC + "\n" + s"mdString_pw_muc_b3: $mdStringOrig" + "\n\n" + precisionRecallF1 + "\n\n"
  }


  def writeClustering(clustering: Iterable[(String, String)], file: File) = {
    val pw = new PrintWriter(file, "UTF-8")
    clustering.foreach {
      pr =>
        pw.println(pr._1 + "\t" + pr._2)
        pw.flush()
    }
    pw.close()
  }

  lazy val truePositiveHTML = evaluation.truePositivesReport(algorithm.getMention)
  lazy val falsePositiveHTML = evaluation.falsePositivesReport(algorithm.getMention)
  lazy val falseNegativeHTML = evaluation.falseNegativesReport(algorithm.getMention)
  lazy val clusteringHTML = evaluation.clusterHTMLReport(algorithm.clusterIds.groupBy(_._2).mapValues(_.map(_._1)).values,algorithm.getMention, "Predicted Clustering")
  
  
  def performExperiment(outputDir: File, writeHTML: Boolean = false, numThreads: Int) = {
    if (numThreads == 1) algorithm.execute() else algorithm.executePar(numThreads)
    writeClustering(algorithm.clusterIds, new File(outputDir, "predicted_clustering.txt"))
    writeClustering(goldClustering, new File(outputDir, "gold_clustering.txt"))
    val pw1 = new PrintWriter(new File(outputDir, "results.txt"))
    pw1.println(results)
    pw1.close()

    if (writeHTML) {

      val pw2 = new PrintWriter(new File(outputDir, "clustering.html"))
      pw2.println(clusteringHTML)
      pw2.close()
      
      val pw3 = new PrintWriter(new File(outputDir, "falsePositives.html"))
      pw3.println(falsePositiveHTML)
      pw3.close()

      val pw4 = new PrintWriter(new File(outputDir, "falseNegatives.html"))
      pw4.println(falseNegativeHTML)
      pw4.close()
    }
  }
}

trait IndexableMentions[MentionType <: CorefMention with HTMLFormatting] {
  def getMention(id: String): MentionType
}
