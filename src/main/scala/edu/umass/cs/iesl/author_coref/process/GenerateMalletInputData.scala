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

import java.io.File

import cc.factorie.util.DefaultCmdOptions

import edu.umass.cs.iesl.author_coref._
import edu.umass.cs.iesl.author_coref.data_structures.coreference.AuthorMention
import edu.umass.cs.iesl.author_coref.db.{EmptyDataStore, GenerateAuthorMentionsFromACL}
import edu.umass.cs.iesl.author_coref.load.LoadACL
import edu.umass.cs.iesl.author_coref.utilities.NumThreads

class GenerateMalletInputDataOpts extends DefaultCmdOptions {
  val outputDir = new CmdOption[String]("output-dir", "data/rexa-mallet-input", "DIRECTORY", "Where to write the output that will be used by Mallet")
  val includeTitle = new CmdOption[Boolean]("include-title", true, "BOOLEAN", "Whether or not to include the title")
  val lowercase = new CmdOption[Boolean]("lowercase", true, "BOOLEAN", "Whether or not to set text to lower case")
  val removePunct = new CmdOption[Boolean]("remove-punctuation", true, "BOOLEAN", "Whether or not remove punctuation.")
}


object GenerateMalletInputData {

  def processPar(mentionStreams: Iterable[Iterator[AuthorMention]],outputDir: File, includeTitle: Boolean, lowercase: Boolean, removePunct: Boolean) =
    mentionStreams.par.foreach(processMentions(_,outputDir,includeTitle,lowercase,removePunct))
  
  def processMentions(mentions: Iterator[AuthorMention],outputDir: File, includeTitle: Boolean, lowercase: Boolean, removePunct: Boolean) = {
    mentions.foreach(m => processMention(m,outputDir,includeTitle,lowercase,removePunct))
  }
  
  def processMention(mention: AuthorMention, outputDir: File, includeTitle: Boolean, lowercase: Boolean, removePunct: Boolean) = {
    val text = generateTextFromMention(mention,includeTitle,lowercase,removePunct)
    val fn = mention.mentionId.value
    text.writeToFile(new File(outputDir,fn))
  }

  def generateTextFromMention(mention: AuthorMention, includeTitle: Boolean, lowercase: Boolean, removePunct: Boolean) = {
    val text = new StringBuilder(1000)
    if (includeTitle && mention.title.opt.isDefined) text append mention.title.value + " "
    if (mention.text.opt.isDefined) text append mention.text.value 
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


class GenerateMalletInputDataFromACLOpts extends GenerateMalletInputDataOpts with NumThreads {
  val aclDir = new CmdOption[String]("acl-dir", "The input ACL files", true)
  val aclFileCodec = new CmdOption[String]("acl-file-codec", "UTF-8", "STRING", "The file encoding of the acl files.")
}


object GenerateMalletInputDataFromACL {
  
  def main(args: Array[String]): Unit = {
    val opts = new GenerateMalletInputDataFromACLOpts
    opts.parse(args)
    // Load the papers
    val paperStreams = LoadACL.fromDir(new File(opts.aclDir.value),opts.numThreads.value,printMessages = true, codec = opts.aclFileCodec.value)
    // Expand into ACL Mentions
    val aclmentionStreams = paperStreams.map(_.flatMap(f => f.toACLAuthorMentions))
    // Convert into AuthorMentions
    val authorMentionStreams = aclmentionStreams.map(GenerateAuthorMentionsFromACL.processAll(_,new EmptyDataStore[String,String](),new EmptyDataStore[String,String]()))
    new File(opts.outputDir.value).mkdirs()
    GenerateMalletInputData.processPar(authorMentionStreams,new File(opts.outputDir.value),opts.includeTitle.value,opts.lowercase.value,opts.removePunct.value)
  }
  
}