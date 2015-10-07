package edu.umass.cs.iesl.author_coref.process

import java.io._

import cc.factorie.util.JsonCubbieConverter
import edu.umass.cs.iesl.author_coref.data_structures.coreference.AuthorMention
import edu.umass.cs.iesl.author_coref.db.{EmptyDataStore, GenerateAuthorMentionsFromACL}
import edu.umass.cs.iesl.author_coref.load.{LoadACL, LoadKeywords, LoadTopics}
import edu.umass.cs.iesl.author_coref.utilities.{CodecCmdOption, NumThreads}

object WriteAuthorMentionsToJSON {

  /**
   * Convert each of the mentions into a JSON string and write the JSON string to a line in the file.
   * @param mentions the mentions to write
   * @param file the file to write the mention JSON string to
   * @param codec the file encoding
   * @param bufferSize the buffer size, flush every bufferSize
   */
  def write(mentions: Iterator[AuthorMention], file: File, codec: String, bufferSize: Int = 1000) = {
    val writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), codec))
    import org.json4s.jackson.JsonMethods._
    val strings = mentions.map(f => compact(render(JsonCubbieConverter.toJson(f))))
    strings.zipWithIndex.foreach{
      case (json,idx) =>
        writer.write(json)
        writer.write("\n")
        if (idx % bufferSize == 0)
          writer.flush()
    }
    writer.close()
  }

}


class WriteAuthorMentionsToJSONFromACLOpts extends CodecCmdOption with NumThreads {
  val aclDir = new CmdOption[String]("acl-dir", "The directory containing the Grobid formatted ACL files", true)
  val aclFileCodec = new CmdOption[String]("acl-file-codec", "UTF-8", "STRING", "The file encoding of the ACL files. Note that the normal codec cmd line option will be used for all other files.")
  val bufferSize = new CmdOption[Int]("buffer-size", 1000, "INT", "The size of the buffer to use")
  val topicsFile = new CmdOption[String]("topics-file", "The file containing the topics for each paper", false)
  val keywordsFile = new CmdOption[String]("keywords-file", "The file containing the keywords for each paper", false)
  val outputFile = new CmdOption[String]("output-file", "The file to write the JSON records to.", true)
}

object WriteAuthorMentionsToJSONFromACL {

  def main(args: Array[String]): Unit = {
    val opts = new WriteAuthorMentionsToJSONFromACLOpts
    opts.parse(args)

    // Load the Topic and Keyword files
    val topicsDB = if (opts.topicsFile.wasInvoked && opts.topicsFile.value.nonEmpty) {
      println("Loading Topics File")
      LoadTopics.load(new File(opts.topicsFile.value),opts.codec.value)
    } else
      new EmptyDataStore[String,String]()

    val keywordsDB = if (opts.keywordsFile.wasInvoked && opts.keywordsFile.value.nonEmpty) {
      println("Loading Keywords File")
      LoadKeywords.load(new File(opts.keywordsFile.value),opts.codec.value)
    } else
      new EmptyDataStore[String,String]()

    // Load the papers
    val papers = LoadACL.fromDir(new File(opts.aclDir.value),1,printMessages = false, codec = opts.aclFileCodec.value).head
    // Expand into ACL Mentions
    val aclMentions = papers.flatMap(f => f.toACLAuthorMentions)
    // Convert into AuthorMentions
    val authorMentions = GenerateAuthorMentionsFromACL.processAll(aclMentions,topicsDB,keywordsDB)
    WriteAuthorMentionsToJSON.write(authorMentions,new File(opts.outputFile.value),opts.codec.value,opts.bufferSize.value)
  }

}