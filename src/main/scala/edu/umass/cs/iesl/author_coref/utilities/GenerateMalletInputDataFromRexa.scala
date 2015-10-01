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

package edu.umass.cs.iesl.author_coref.utilities

import java.io.File

import cc.factorie.util.DefaultCmdOptions
import edu.umass.cs.iesl.author_coref.data_structures.RexaAuthorMention

import edu.umass.cs.iesl.author_coref._
import edu.umass.cs.iesl.author_coref.load.LoadRexa

class GenerateMalletInputDataFromRexaOpts extends DefaultCmdOptions {
  val rexaDir = new CmdOption[String]("rexa-dir", "data/rexa", "DIRECTORY", "Rexa data")
  val outputDir = new CmdOption[String]("output-dir", "data/rexa-mallet-input", "DIRECTORY", "Where to write the output that will be used by Mallet")
  val includeTitle = new CmdOption[Boolean]("include-title", true, "BOOLEAN", "Whether or not to include the title")
  val includeAbstract = new CmdOption[Boolean]("include-abstract", true, "BOOLEAN", "Whether or not to include the abstract")
  val includeBody = new CmdOption[Boolean]("include-body", false, "BOOLEAN", "Whether or not to include the body")
  val lowercase = new CmdOption[Boolean]("lowercase", true, "BOOLEAN", "Whether or not to set text to lower case")
  val removePunct = new CmdOption[Boolean]("remove-punctuation", true, "BOOLEAN", "Whether or not remove punctuation.")
}


object GenerateMalletInputDataFromRexa {

  
  def main(args: Array[String]) = {
    val opts = new GenerateMalletInputDataFromRexaOpts()
    opts.parse(args)
    new File(opts.outputDir.value).mkdirs()
    val mentions = LoadRexa.fromDir(new File(opts.rexaDir.value),"UTF-8")
    mentions.foreach(m => processMention(m,new File(opts.outputDir.value),opts.includeTitle.value,opts.includeAbstract.value,opts.includeBody.value,opts.lowercase.value,opts.removePunct.value))
  }
  
  
  def processMention(mention: RexaAuthorMention, outputDir: File, includeTitle: Boolean, includeAbstract: Boolean, includeBody: Boolean, lowercase: Boolean, removePunct: Boolean) = {
    val text = generateTextFromMention(mention,includeTitle,includeAbstract,includeBody,lowercase,removePunct)
    val fn = mention.id
    text.writeToFile(new File(outputDir,fn))
  }
  
  def generateTextFromMention(mention: RexaAuthorMention, includeTitle: Boolean, includeAbstract: Boolean, includeBody: Boolean, lowercase: Boolean, removePunct: Boolean) = {
    val text = new StringBuilder(1000)
    if (includeTitle && mention.title.isDefined) text append mention.title.get + " "
    if (includeAbstract && mention.abstractText.isDefined) text append mention.abstractText.get + " "
    if (includeBody && mention.body.isDefined) text append mention.body.get + " "
    if (lowercase && removePunct)
      text.toString().toLowerCase.removePunctuation(" ")
    else if (lowercase)
      text.toString().toLowerCase
    else if (removePunct)
      text.toString().removePunctuation(" ")
    else 
      text.toString()
  }
  
}
