package edu.umass.cs.iesl.author_coref.process

import java.io.{FileOutputStream, OutputStreamWriter, BufferedWriter, File}

import cc.factorie.util.DefaultCmdOptions
import edu.umass.cs.iesl.author_coref._
import edu.umass.cs.iesl.author_coref.data_structures.coreference.AuthorMention
import edu.umass.cs.iesl.author_coref.load.LoadJSONAuthorMentions
import edu.umass.cs.iesl.author_coref.utilities.CodecCmdOption

class GenerateWordEmbeddingTrainingOpts extends DefaultCmdOptions with CodecCmdOption{
  val outputFile = new CmdOption[String]("output-file", "Where to write the output data", true)
  val removePunctuation = new CmdOption[Boolean]("remove-punctuation", true, "BOOLEAN", "Whether or not to remove punctuation")
  val lowercase = new CmdOption[Boolean]("lowercase", true, "BOOLEAN", "Whether or not to lowercase all text")
}
object GenerateWordEmbeddingTrainingData {

  def processText(text: String, removePunct: Boolean, lowercase: Boolean) = {
    var res = text
    if (removePunct) res = res.removePunctuation()
    if (lowercase) res = res.toLowerCase
    res
  }

  def getText(authorMention: AuthorMention, removePunct: Boolean, lowercase: Boolean): Option[String] =
    (authorMention.title.opt.getOrElse("") + " " + authorMention.text.opt.getOrElse("")).trimBegEnd().noneIfEmpty
      .map(processText(_,removePunct,lowercase))

  def getTexts(authorMentions: Iterator[AuthorMention], removePunct: Boolean, lowercase: Boolean) =
    authorMentions.flatMap(getText(_,removePunct,lowercase))

}


class GenerateWordEmbeddingTrainingFromJSONOpts extends GenerateWordEmbeddingTrainingOpts {
  val jsonFile = new CmdOption[String]("json-file", "The json serialized mentions", true)
}

object GenerateWordEmbeddingTrainingDataFromJSON {

  def main(args: Array[String]): Unit = {
    val opts = new GenerateWordEmbeddingTrainingFromJSONOpts()
    opts.parse(args)

    val mentions = LoadJSONAuthorMentions.load(new File(opts.jsonFile.value),opts.codec.value)
    val texts = GenerateWordEmbeddingTrainingData.getTexts(mentions,opts.removePunctuation.value,opts.lowercase.value)

    val writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(opts.outputFile.value)),opts.codec.value))
    texts.foreach{
      t =>
        writer.write(t)
        writer.write("\n")
        writer.flush()
    }
    writer.close()
  }
}

