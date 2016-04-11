package edu.umass.cs.iesl.author_coref.load

import java.io.File

import cc.factorie.util.JsonCubbieConverter
import edu.umass.cs.iesl.author_coref._
import edu.umass.cs.iesl.author_coref.data_structures.coreference.AuthorMention
import org.json4s.JsonAST.JObject
import org.json4s.NoTypeHints
import org.json4s.jackson.JsonMethods._
import org.json4s.jackson.Serialization

object LoadJSONAuthorMentions {

  implicit val formats = Serialization.formats(NoTypeHints)

  def loadLine(string: String): Option[AuthorMention] =
    parseOpt(string).map(jvalue => JsonCubbieConverter.toCubbie(jvalue.asInstanceOf[JObject], () => new AuthorMention()))

  def loadLines(lines: Iterator[String]) = lines.flatMap(loadLine)

  def load(file: File, codec: String) = {
    println(s"[LoadJSONAuthorMentions] Loading from ${file.getAbsolutePath}")
    loadLines(file.lines(codec))
  }

  def load(file: File, codec: String, start: Int, end: Int) = loadLines(file.lines(codec,start,end))

  def load(file: File, codec: String, start: Int) = loadLines(file.lines(codec,start))

  def loadMultiple(file: File, codec: String, numThreads: Int, numLines: Option[Int] = None) = {
    val start = System.currentTimeMillis()
    val numLinesInFile = if (numLines.nonEmpty) numLines.get else file.numLines
    val end = System.currentTimeMillis()
    println(s"[${this.getClass.ordinaryName}] There are $numLinesInFile in ${file.getName} (found in ${end-start} ms)")
    val blockSize = numLinesInFile/numThreads
    println(s"[${this.getClass.ordinaryName}] Each of the $numThreads iterators will have about $blockSize items")
    val startingIndices = (0 until numThreads).map(_ * blockSize)
    startingIndices.dropRight(1).zip(startingIndices.drop(1)).map(f => load(file,codec,f._1,f._2)) ++ Iterable(load(file,codec,startingIndices.last))
  }

  def fromFiles(files: Iterable[File], codec: String) = files.map(f => LoadJSONAuthorMentions.load(f,codec))

  def fromDir(dir: File,codec: String) = {
    println(s"[LoadJSONAuthorMentions] Loading from directory ${dir.getAbsolutePath}")
    fromFiles(dir.listFiles.filterNot(_.getName.startsWith("\\.")),codec)
  }

}
