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

package edu.umass.cs.iesl.author_coref.embedding

import java.io.{PrintWriter, File}

import cc.factorie.util.DefaultCmdOptions
import edu.umass.cs.iesl.author_coref.load.LoadRexa
import edu.umass.cs.iesl.author_coref.utilities.CodecCmdOption
import edu.umass.cs.iesl.author_coref._

class GenerateEmbeddingTrainingDataFromRexaOpts extends DefaultCmdOptions with CodecCmdOption{
  val rexaDir = new CmdOption[String]("rexa-dir","The top level directory of the Rexa data", true)
  val outputFile = new CmdOption[String]("output-file", "Where to write the output data", true)
  val removePunctuation = new CmdOption[Boolean]("remove-punctuation", true, "BOOLEAN", "Whether or not to remove punctuation")
  val lowercase = new CmdOption[Boolean]("lowercase", true, "BOOLEAN", "Whether or not to lowercase all text")
}


object GenerateEmbeddingTrainingDataFromRexa {
  
  def main(args: Array[String]) = {
    val opts = new GenerateEmbeddingTrainingDataFromRexaOpts()
    opts.parse(args)
    
    val mentions = LoadRexa.fromDir(new File(opts.rexaDir.value),opts.codec.value)
    
    val outputFile = new File(opts.outputFile.value)
    
    val pw = new PrintWriter(outputFile,opts.codec.value)
    
    def processAndPrint(t: String) = pw.println(processText(t,opts.removePunctuation.value,opts.lowercase.value))
    
    mentions.foreach{
      m =>
        if (m.title.nonEmpty)
          processAndPrint(m.title.get)
        if (m.altTitle.nonEmpty)
          processAndPrint(m.altTitle.get)
        if (m.abstractText.nonEmpty)
          processAndPrint(m.abstractText.get)
        if (m.body.nonEmpty)
          processAndPrint(m.body.get)
        pw.flush()
    }
    pw.close()
    
  }
  
  def processText(text: String, removePunct: Boolean, lowercase: Boolean) = {
    var res = text
    if (removePunct) res = res.removePunctuation()
    if (lowercase) res = res.toLowerCase
    res
  }
  
  
}
