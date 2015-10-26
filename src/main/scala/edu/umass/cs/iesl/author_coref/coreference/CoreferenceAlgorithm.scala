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
import edu.umass.cs.iesl.author_coref.data_structures.Author
import edu.umass.cs.iesl.author_coref.data_structures.coreference.{AuthorMention, CorefAuthorVars, CorefMention}
import edu.umass.cs.iesl.author_coref.experiment.IndexableMentions
import cc.factorie._
import scala.collection.JavaConverters._

import scala.util.Random


/**
 * Base trait for algorithms that perform coreference.
 * Defines interface to run coreference and return predicted clustering.
 * @tparam MentionType
 */
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

/**
 * A trait defining a canopy for a mention which can be altered
 */
trait MutableSingularCanopy extends SingularCanopy {

  /**
   * The canopy to which the mention belongs.
   */
  var canopy:String
}

/**
 * Base trait for algorithms performing hierarchical coreference.
 * @tparam V the data type of the variables stored by the nodes of the tree structure
 * @tparam CanopyInput the data type that is used to derive the canopy
 */
trait HierarchicalCorefSystem[V <: NodeVariables[V] with MutableSingularCanopy,CanopyInput] {

  /**
   * The name of the coreference algorithm
   */
  val name = this.getClass.ordinaryName


  /**
   * The mentions known to the algorithm
   * @return
   */
  def mentions: Iterable[Mention[V]]


  /**
   * The model that will be used. The model stores things such as the
   * feature templates and associated parameters.
   * @return
   */
  def model: CorefModel[V]

  /**
   * Estimate the number of iterations required
   * @param ments estimate based on these mentions
   * @return
   */
  def estimateIterations(ments: Iterable[Node[V]]) = math.min(ments.size * 30.0, 1000000.0).toInt

  /**
   * Return the sampler that will be used to perform inference on the given
   * mentions.
   * @param mentions the group of mentions that will be used to construct the sampler
   * @return
   */
  def sampler(mentions:Iterable[Node[V]]): CorefSampler[V]

  /**
   * Multiple canopy functions can be used in succession with inference executed in each round.
   * This function decides which tree nodes will be used in the next round of inference.
   * Alternatives include: all nodes; only the root nodes; etc
   * @param mentions the set of mention nodes (leaves)
   * @return
   */
  def determineNextRound(mentions: Iterable[Node[V]]): Iterable[Node[V]]

  /**
   * A canopy function is a conversion from the CanopyInput to a String canopy ID.
   */
  type CanopyFunction = CanopyInput => String

  /**
   * The ordered group of canopy functions. The functions
   * are applied in succession with determineNextRound specifying
   * which nodes will be used for inference in the next round.
   * @return
   */
  def canopyFunctions: Iterable[CanopyFunction] //TODO: Make this a Seq

  /**
   * Of the succession of canopy functions, the canopy function which is the most general
   * in terms of containment. For example, if the input to canopy function is a first name and
   * last name pair and there were two canopy functions, 1) the first three characters of the first name and last name
   * and 2) the last name. The latter function would be the more general. The most general function
   * must meet the requirement that it divide the data into non-overlapping partitions.
   * @return
   */
  def mostGeneralFunction: CanopyFunction = canopyFunctions.last

  /**
   * Given the variables stored in a node, return the data that will be used to determine the canopy.
   * @param vars the variables that will be used
   * @return
   */
  def getCanopyInput(vars: V): CanopyInput

  /**
   * Given a canopy function and the mention nodes, assign each mention to a particular canopy
   * then propagate the canopy assignment to the ancestors of the mentions. Each node is assigned a
   * single canopy in the current model, but in the future this could be extended to allow nodes
   * to be assigned to multiple canopies.
   * @param function the canopy function
   * @param mentions the mentions
   */
  def assignCanopy(function: CanopyFunction, mentions: Iterable[Mention[V]]): Unit = mentions.foreach{
    case mention =>
      val canopy = function(getCanopyInput(mention.variables))
      mention.variables.canopy = canopy
      propagateAssignment(canopy,mention.getParent)
  }

  /**
   * Helper function for propagating the canopy assignment of nodes up the three structure
   * @param canopy the canopy
   * @param node the current node
   */
  def propagateAssignment(canopy: String, node: Option[Node[V]]): Unit = if (node.isDefined) {
    node.get.variables.canopy = canopy
    propagateAssignment(canopy, node.get.getParent)
  }

  /**
   * Perform inference on the given group of nodes and then all subgroups of nodes defined by the canopy functions.
   * Note that canopy functions which do not change the division of mentions will be skipped.
   * @param canopyFunctions the canopy functions
   * @param groupNodes the current group of nodes on which to run inference
   * @param mentions the original set of mention nodes
   */
  def performInferenceInGroups(canopyFunctions: Iterable[CanopyFunction], groupNodes: Iterable[Node[V]], mentions: Iterable[Mention[V]]): Unit = {
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
        performInferenceInGroups(canopyFunctions.drop(1),determineNextRound(groupNodes),mentions)
    }
  }

  /**
   * Execute coreference on all of the mentions in a single thread
   */
  def run() = {
    println(s"[$name] Running Coreference Experiment.")
    val groupped = mentions.groupBy(m => mostGeneralFunction(getCanopyInput(m.variables)))
    val numTotalCanopies = groupped.size
    groupped.toIterable.zipWithIndex.foreach {
      case ((group, mentionsInGroup),idx) =>
        println(s"[$name] Processing Group #${idx+1} of $numTotalCanopies, $group, with ${mentionsInGroup.size} mentions")
        performInferenceInGroups(canopyFunctions,mentionsInGroup,mentionsInGroup)
    }
  }

  /**
   * Execute coreference on all of the mentions in multiple threads
   * The data is parallelized by the most general canopy function.
   * @param numThreads the number of threads to use
   */
  def runPar(numThreads: Int) = {
    println(s"[$name] Running Coreference Experiment.")
    val groupped = mentions.groupBy(m => mostGeneralFunction(getCanopyInput(m.variables)))
    val numTotalCanopies = groupped.size
    groupped.toIterable.zipWithIndex.grouped(numThreads).toIterable.par.foreach(_.foreach {
      case ((group, mentionsInGroup),idx) =>
        println(s"[$name] Processing Group #${idx + 1} of $numTotalCanopies, $group, with ${mentionsInGroup.size} mentions")
        performInferenceInGroups(canopyFunctions,mentionsInGroup,mentionsInGroup)
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
    with DebugCoref[CorefAuthorVars] with PrintlnLogger {
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


class DeterministicBaselineCoreferenceAlgorithm(override val mentions: Iterable[AuthorMention], canopyFunction: AuthorMention => String) extends PairwiseCoreferenceAlgorithm(mentions,canopyFunction) {

  def maxAcceptableEditDistance = 5
  def approxFirstNameMatch(fn1: String, fn2: String): Boolean = {
    (fn1 editDistance fn2) < maxAcceptableEditDistance
  }

  def restrictToNonInitialFirstNames: Boolean = true

  def nonInitialFirstNames(m1: AuthorMention,m2: AuthorMention) = firstNameNotInitial(m1.self.value) && firstNameNotInitial(m2.self.value)

  def firstNameNotInitial(author: Author) = author.firstName.isDefined && author.firstName.value.length > 1

  def areCoreferent(mention1: AuthorMention, mention2: AuthorMention) = {
    mention1.canopy.value == mention2.canopy.value && // same canopy
      mention1.self.value.lastName.value == mention2.self.value.lastName.value && // last name match
      approxFirstNameMatch(mention1.self.value.firstName.value, mention2.self.value.firstName.value) && // first name match
    mention1.self.value.middleInitials == mention2.self.value.middleInitials && // middle name match
      (mention1.coAuthorStrings.toSet intersect mention2.coAuthorStrings.toSet).nonEmpty &&
      (!restrictToNonInitialFirstNames || nonInitialFirstNames(mention1,mention2))
  }
}
