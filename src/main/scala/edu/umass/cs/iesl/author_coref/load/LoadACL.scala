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

import edu.umass.cs.iesl.author_coref._

import java.io.{FileInputStream, InputStreamReader, File}

import cc.factorie.util.NonValidatingXML
import edu.umass.cs.iesl.author_coref.data_structures.{Venue, Date, Author, ACLPaper}

import scala.collection.mutable.ArrayBuffer
import scala.xml.{NodeSeq, Elem}


object LoadACL {

  def fromDir(dir: File, numThreads: Int, printMessages: Boolean = true, codec: String = "UTF-8", fileFilter: File => Boolean = _ => true): Iterable[Iterator[ACLPaper]] = fromFiles(dir.listFiles().filter(fileFilter),codec, numThreads,printMessages)

  def fromFiles(files: Iterable[File], codec: String = "UTF-8", numThreads: Int, printMessages: Boolean = true): Iterable[Iterator[ACLPaper]] = {
    val numFiles = files.size
    @volatile var errors = new ArrayBuffer[String]()
    
    val groupSize = Math.ceil(numFiles.toDouble / numThreads.toDouble).toInt
    val groups = files.grouped(groupSize).toIterable

    val res = groups.zipWithIndex.map{case (group,groupIdx) => group.toIterator.zipWithIndex.flatMap{case (p,idx) =>
      if (printMessages) println(s"[Loader] Group $groupIdx Loading from ${p.getName} (Loaded: ${idx+1}/$numFiles, Num Errors: ${errors.length}).")
      try {
        fromFile(p,codec)
      } catch {
        case e: Exception =>
          errors += p.getName
          if (printMessages) println("   ERROR: " + e.getMessage)
          None
      }
    }}
    println()
    res
  }
  
  def fromFile(file: File, codec: String = "UTF-8"): Iterable[ACLPaper] = {
    val xml = NonValidatingXML.load(new InputStreamReader(new FileInputStream(file), codec))
    val paperId = file.getNameWithoutExtension
    loadSingle(xml,paperId)
  }
  
  def loadSingle(xml: Elem, paperId: String): Option[ACLPaper] = {
    val header = xml \ "teiHeader"
    val title = (header \\ "titleStmt" \\ "title").map(_.text).headOption.getOrElse("")
    val authors = getAuthors(header)
    val date = getDate(header  \\  "publicationStmt")
    val venue = new Venue((header \\ "sourceDesc" \\ "monogr" \\ "title").map(_.text).headOption.getOrElse(""))
    val abstractText = (header \\ "profileDesc" \\ "abstract").map(_.text).headOption.getOrElse("")
    val aclPaper = new ACLPaper(paperId,Some(title),Some(Seq(venue)),Some(date),Some(authors),Some(abstractText))
    Some(aclPaper)
  }

  private def getDate(nodeSeq: NodeSeq) = Date((nodeSeq  \\ "date").map((p) => ((p \\ "@when").text, (p \\ "@type").text)).filter(_._2 == "published").map(_._1).headOption.getOrElse("")).year

  private def getAuthors(nodeSeq: NodeSeq) =
    (nodeSeq \\ "author").map(_ \\ "persName").map(getAuthor)

  private def getAuthor(nodeSeq: NodeSeq) = {
    val lastName = (nodeSeq \\ "surname").map(_.text).headOption.getOrElse("")
    val firstAndMiddle = (nodeSeq \\ "forename").map((n) => (n \\ "@type").text -> n.text).groupBy(_._1).mapValues(_.map(_._2))
    new Author(firstAndMiddle.getOrElse("first",Seq("")).head, firstAndMiddle.getOrElse("middle",Seq()), lastName)
  }
  

}
