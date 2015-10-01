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

import java.util

import cc.factorie.app.nlp.hcoref._
import cc.factorie.util.Threading
import cc.factorie.variable.DiffList
import edu.umass.cs.iesl.author_coref._
import edu.umass.cs.iesl.author_coref.data_structures.coreference.{AuthorMention, CorefAuthorVars, CorefMention}
import edu.umass.cs.iesl.author_coref.experiment.IndexableMentions
import cc.factorie._
import scala.collection.JavaConverters._

import scala.util.Random

trait CoreferenceAlgorithm[MentionType <: CorefMention] {

  val name = this.getClass.ordinaryName

  /**
   * The mentions known to the algorithm
   * @return
   */
  def mentions: Iterable[MentionType]

  /**
   * Run the algorithm 
   */
  def execute(): Unit
  
  /**
   * Run the algorithm using numThreads
   * @param numThreads - number of threads to use
   */
  def executePar(numThreads: Int)


  /**
   * Return pairs of mentionIds and entityIds
   * @return
   */
  def clusterIds: Iterable[(String,String)]
}

trait MutableSingularCanopy extends SingularCanopy {
  var canopy:String
}

trait HierarchicalCorefSystem[V <: NodeVariables[V] with MutableSingularCanopy,CanopyInput] {
  
  val name = this.getClass.ordinaryName

  def mentions: Iterable[Mention[V]]

  def model: CorefModel[V]

  def estimateIterations(ments: Iterable[Node[V]]) = math.min(ments.size * 30.0, 1000000.0).toInt

  def sampler(mentions:Iterable[Node[V]]): CorefSampler[V]

  def determineNextRound(mentions: Iterable[Node[V]]): Iterable[Node[V]]

  type CanopyFunction = CanopyInput => String

  def canopyFunctions: Iterable[CanopyFunction]

  def mostGeneralFunction: CanopyFunction = canopyFunctions.last

  def getCanopyInput(vars: V): CanopyInput

  def assignCanopy(function: CanopyFunction, mentions: Iterable[Mention[V]]): Unit = mentions.foreach{
    case mention =>
      val canopy = function(getCanopyInput(mention.variables))
      mention.variables.canopy = canopy
      propagateAssignment(canopy,mention.getParent)
  }

  def propagateAssignment(canopy: String, node: Option[Node[V]]): Unit = if (node.isDefined) {
    node.get.variables.canopy = canopy
    propagateAssignment(canopy, node.get.getParent)
  }

  def runInGroup(canopyFunctions: Iterable[CanopyFunction], groupNodes: Iterable[Node[V]], mentions: Iterable[Mention[V]]): Unit = {
    if (canopyFunctions.nonEmpty) {
      val cf = canopyFunctions.head
      assignCanopy(cf,mentions)
      val subgroups = groupNodes.groupBy(m => m.variables.canopy)
      subgroups.foreach {
        case (subgroup, mentionsInSubGroup) =>
          try {
            println(s"[$name] Applying canopy function: $cf")
            val start = System.currentTimeMillis()
            sampler(mentionsInSubGroup).infer()
            println(s"[$name] Finished Coref. Total time: ${System.currentTimeMillis() - start} ms")
          } catch {
            case e: Exception =>
              println(s"[$name] Failure:")
              e.printStackTrace()
          }
      }
      val numCanopiesThisRound = groupNodes.map(_.variables.canopy).toSet.size
      // If there was only one canopy in this round, then we are done. The next clustering by canopy
      // will be identical to this one.
      if (numCanopiesThisRound != 1)
        runInGroup(canopyFunctions.drop(1),determineNextRound(groupNodes),mentions)
    }
  }

  def run() = {
    println(s"[$name] Running Coreference Experiment.")
    val groupped = mentions.groupBy(m => mostGeneralFunction(getCanopyInput(m.variables)))
    val numTotalCanopies = groupped.size
    groupped.toIterable.zipWithIndex.foreach {
      case ((group, mentionsInGroup),idx) =>
        println(s"[$name] Processing Group #${idx+1} of $numTotalCanopies, $group, with ${mentionsInGroup.size} mentions")
        runInGroup(canopyFunctions,mentionsInGroup,mentionsInGroup)
    }
  }

  def runPar(numThreads: Int) = {
    println(s"[$name] Running Coreference Experiment.")
    val groupped = mentions.groupBy(m => mostGeneralFunction(getCanopyInput(m.variables)))
    val numTotalCanopies = groupped.size
    groupped.toIterable.zipWithIndex.grouped(numThreads).toIterable.par.foreach(_.foreach {
      case ((group, mentionsInGroup),idx) =>
        println(s"[$name] Processing Group #${idx + 1} of $numTotalCanopies, $group, with ${mentionsInGroup.size} mentions")
        runInGroup(canopyFunctions,mentionsInGroup,mentionsInGroup)
    })
  }
  


}


class StandardHCorefSystem(opts: AuthorCorefModelOptions, val mentions: Iterable[Mention[CorefAuthorVars]], override val canopyFunctions: Iterable[AuthorMention => String]) extends HierarchicalCorefSystem[CorefAuthorVars,AuthorMention] {
  
  implicit val random = new Random(0)
  
  override def model: CorefModel[CorefAuthorVars] = AuthorCorefModel.fromCmdOptions(opts)

  override def getCanopyInput(vars: CorefAuthorVars): AuthorMention = vars.provenance.get

  override def determineNextRound(mentions: Iterable[Node[CorefAuthorVars]]): Iterable[Node[CorefAuthorVars]] =
    mentions.map(_.root).stableRemoveDuplicates()


  override def sampler(mentions: Iterable[Node[CorefAuthorVars]]): CorefSampler[CorefAuthorVars] =  new CorefSampler[CorefAuthorVars](model, mentions, estimateIterations(mentions))
    with AutoStoppingSampler[CorefAuthorVars]
    with CanopyPairGenerator[CorefAuthorVars]
    with AuthorCorefMoveGenerator[CorefAuthorVars]
    with DebugCoref[CorefAuthorVars] {
    def autoStopThreshold = 50000
    def newInstance(implicit d: DiffList) = new Node[CorefAuthorVars](new CorefAuthorVars)
  }

}


class HierarchicalCoreferenceAlgorithm(opts: AuthorCorefModelOptions, override val mentions: Iterable[AuthorMention], keystore: Keystore, canopyFunctions: Iterable[AuthorMention => String]) extends CoreferenceAlgorithm[AuthorMention]  with IndexableMentions[AuthorMention]{

  lazy val mentionMap = mentions.groupBy(_.mentionId.value).mapValues(_.head)

  override def getMention(id: String): AuthorMention = mentionMap(id)

  val hcorefMentions = mentions.map(_.toMentionNode(keystore))
  
  /**
   * Run the algorithm
   */
  override def execute(): Unit = new StandardHCorefSystem(opts,hcorefMentions,canopyFunctions).run()

  /**
   * Run the algorithm using numThreads
   * @param numThreads - number of threads to use
   */
  override def executePar(numThreads: Int): Unit =  new StandardHCorefSystem(opts,hcorefMentions,canopyFunctions).runPar(numThreads)

  /**
   * Return pairs of mentionIds and entityIds
   * @return
   */
  override def clusterIds: Iterable[(String, String)] = hcorefMentions.map(m => (m.uniqueId,m.entity.uniqueId))
}


abstract class PairwiseCoreferenceAlgorithm(override val mentions: Iterable[AuthorMention], canopyFunction: AuthorMention => String) extends CoreferenceAlgorithm[AuthorMention] with IndexableMentions[AuthorMention]{
  
  def areCoreferent(mention1: AuthorMention, mention2: AuthorMention): Boolean
  
  private def executeCanopy(canopyName: String, canopyMentions: Iterable[AuthorMention]) = {
    println(s"[${this.getClass.ordinaryName}] Processing canopy $canopyName with ${canopyMentions.size} mentions")
    val pairs = canopyMentions.pairs
    val decisions = pairs.map(f => areCoreferent(f._1,f._2))
    val clusterId2entries = new util.HashMap[String,scala.collection.mutable.Set[String]]().asScala
    val mentionId2cluster = new util.HashMap[String,String]().asScala
    pairs.zip(decisions).foreach {
      case (pair,coreferent) =>
        if (coreferent) {
          val m1 = pair._1.mentionId.value
          val m2 = pair._2.mentionId.value
          // Both are singletons, create a new cluster
          if (!mentionId2cluster.contains(m1) && !mentionId2cluster.contains(m2)) {
            val clusterId = new Random().alphanumeric.take(20).mkString("")
            mentionId2cluster.put(m1,clusterId)
            mentionId2cluster.put(m2,clusterId)
            clusterId2entries.put(clusterId, new util.HashSet[String]().asScala)
            clusterId2entries(clusterId).add(m1)
            clusterId2entries(clusterId).add(m2)
            // M1 is in a cluster, m2 is not
          } else if (mentionId2cluster.contains(m1) && !mentionId2cluster.contains(m2)) {
            val clusterId = mentionId2cluster(m1)
            mentionId2cluster.put(m2,clusterId)
            clusterId2entries(clusterId).add(m2)
            // M2 is in a cluster m1 is not
          } else if (!mentionId2cluster.contains(m1) && mentionId2cluster.contains(m2)) {
            val clusterId = mentionId2cluster(m2)
            mentionId2cluster.put(m1,clusterId)
            clusterId2entries(clusterId).add(m1)
            // else both in clusters, merge
          } else {
            val m1clusterId = mentionId2cluster(m1)
            val m2clusterId = mentionId2cluster(m2)
            if (m1clusterId != m2clusterId) {
              val m2clusterEntries = clusterId2entries(m2clusterId)
              clusterId2entries.remove(m2clusterId)
              val m1set = clusterId2entries(m1clusterId)
              m2clusterEntries.foreach { e => mentionId2cluster.put(e, m1clusterId); m1set.add(e)}
            }
          }
        }
    }
    mentionId2cluster.foreach{
      case (mid,eid) =>
        mentionMap(mid).entityId.set(eid)
    }
  }
  
  /**
   * Run the algorithm 
   */
  override def execute(): Unit = {
    mentions.foreach{
      m =>
        m.canopy.set(canopyFunction(m))
    }
    mentions.groupBy(_.canopy.value).foreach {
    canopy =>
      executeCanopy(canopy._1,canopy._2)
  }}

  /**
   * Run the algorithm using numThreads
   * @param numThreads - number of threads to use
   */
  override def executePar(numThreads: Int): Unit = {
    mentions.foreach{
      m =>
        m.canopy.set(canopyFunction(m))
    }
    Threading.parForeach(mentions.groupBy(_.canopy.value),numThreads)(
    canopy =>
      executeCanopy(canopy._1,canopy._2))}
  

  /**
   * Return pairs of mentionIds and entityIds
   * @return
   */
  override def clusterIds: Iterable[(String, String)] = mentions.map(m => (m.mentionId.value,m.entityId.opt.getOrElse(m.mentionId.value)))

  lazy val mentionMap = mentions.groupBy(_.mentionId.value).mapValues(_.head)

  override def getMention(id: String): AuthorMention = mentionMap(id)
}

class DeterministicCoreferenceAlgorithm(override val mentions: Iterable[AuthorMention], canopyFunction: AuthorMention => String) extends PairwiseCoreferenceAlgorithm(mentions,canopyFunction) {

  // Group mentions by Canopy
  // Within each canopy, we can take pairs of mentions
  // If share email address, then coreferent
  // If the mentions share at least 2 co-authors, assign as coreferent
  // If the mentions share 8+ topics and 1+ keywords
  def areCoreferent(mention1: AuthorMention, mention2: AuthorMention) = {
    mention1.canopy.value == mention2.canopy.value && // same canopy 
      mention1.self.value.lastName.value == mention2.self.value.lastName.value &&
      mention1.self.value.firstName.value.editDistance(mention2.self.value.firstName.value) < 8 &&
      (
        (mention1.self.value.emails.opt.getOrElse(Seq()).toSet intersect mention2.self.value.emails.opt.getOrElse(Seq()).toSet).nonEmpty || // same email
          (mention1.self.value.institutions.opt.getOrElse(Seq()).toSet intersect mention2.self.value.institutions.opt.getOrElse(Seq()).toSet).nonEmpty || // same institution
          (mention1.coAuthorStrings.toSet intersect mention2.coAuthorStrings.toSet).size >= 2  || // share 1 or more co authors
          ((mention1.topics.opt.getOrElse(Seq()).toSet intersect mention2.topics.opt.getOrElse(Seq()).toSet).size >= 8 && (mention1.keywords.opt.getOrElse(Seq()).toSet intersect mention2.keywords.opt.getOrElse(Seq()).toSet).nonEmpty) // share two topics and a keyword
        )
  }
}