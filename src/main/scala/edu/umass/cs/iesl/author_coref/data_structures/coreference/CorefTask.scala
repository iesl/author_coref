package edu.umass.cs.iesl.author_coref.data_structures.coreference

/**
* A coreference task is an object that stores a collection of ids that will by run
* by the parallel coreference algorithm. Typically, these ids correspond to a single
* canopy group
* @param name - the name of the task
* @param ids - the ids in the task
*/
case class CorefTask(name: String, ids: Iterable[String]) {
  assert(!name.contains("\t"), s"The task name must not contain a tab. The invalid name was $name")
  assert(!ids.exists(_.contains(",")), "None of the ids may contain commas")
  override def toString: String = {
    name + "\t" + ids.mkString(",")
  }
}

object CorefTask{
  /**
   * Parse a raw string and return a coref task
   * @param raw - input string
   * @return the corresponding task
   */
  def apply(raw: String): CorefTask = raw.split("\t") match {
    case Array(name,idString) =>
      CorefTask(name,idString.split(","))
  }
}

/**
 * A coref task with the inventor mentions corresponding to the ids
 * @param name - the name of the task
 * @param ids - the ids in the task
 * @param mentions - the mentions
 */
class CorefTaskWithMentions(name: String, ids: Iterable[String], val mentions: Iterable[AuthorMention]) extends CorefTask(name,ids)
