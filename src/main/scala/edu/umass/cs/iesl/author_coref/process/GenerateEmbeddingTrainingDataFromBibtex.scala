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

package edu.umass.cs.iesl.author_coref.process

import java.io.{PrintWriter, File}

import cc.factorie.app.bib.parser.Dom
import cc.factorie.util.DefaultCmdOptions
import edu.umass.cs.iesl.author_coref._
import edu.umass.cs.iesl.author_coref.utilities.CodecCmdOption


class GenerateEmbeddingTrainingDataFromBibtexOpts extends DefaultCmdOptions with CodecCmdOption {
  val output = new CmdOption[String]("output","Where to write the output", true)
  val inputDir = new CmdOption[String]("input", "Where to read the input data from", true)
}


object GenerateEmbeddingTrainingDataFromBibtex {

  def getData(file: File, codec: String) = {
    println(s"[GenerateEmbeddingTrainingDataFromBibtex] Parsing ${file.getName}")
    val fileContents = file.getStringContents(codec)
    val parsed = Dom.stringToDom(fileContents)
    if (parsed.isLeft) {
      println(s"[GenerateEmbeddingTrainingDataFromBibtex] ERROR while parsing ${file.getName}")
      Iterable()
    } else {
      val domDoc = parsed.right.get
      val res = domDoc.entries.map{
        case (_,entry) =>
          val maybeTitle = entry.otherFields.get("title")
          val maybeAbstract = entry.otherFields.get("abstract")
          val maybeKeywords = entry.otherFields.get("keywords")
          (maybeTitle ++ maybeAbstract ++ maybeKeywords).mkString(" ")
      }
      println(s"[GenerateEmbeddingTrainingDataFromBibtex] Found ${res.size} records")
      res
    }
  }

  def fromFilenames(filenames: Iterator[String], codec: String) = {
    filenames.map(s => getData(new File(s),codec))
  }

  def main(args: Array[String]) = {
    val opts = new GenerateEmbeddingTrainingDataFromBibtexOpts
    opts.parse(args)
    val pw = new PrintWriter(opts.output.value,opts.codec.value)
    val filenames = new File(opts.inputDir.value).list().filterNot(_.startsWith("\\.")).map(new File(opts.inputDir.value,_).getAbsolutePath).toIterator
    val data = fromFilenames(filenames,opts.codec.value)
    data.foreach(_.foreach(pw.println))
    pw.close()
  }


}
