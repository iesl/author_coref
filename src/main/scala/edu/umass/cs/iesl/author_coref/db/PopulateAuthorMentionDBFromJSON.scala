package edu.umass.cs.iesl.author_coref.db

import java.io.File

import edu.umass.cs.iesl.author_coref.load.LoadJSONAuthorMentions
import edu.umass.cs.iesl.author_coref.utilities.{CodecCmdOption, MongoDBOpts, NumThreads}


class PopulateAuthorMentionDBFromJSONOpts extends CodecCmdOption with NumThreads with MongoDBOpts {
  val jsonFile = new CmdOption[String]("json-file", "A json files to read", false)
  val bufferSize = new CmdOption[Int]("buffered-size", "The size of the buffer to use", true)
  val topicsFile = new CmdOption[String]("topics-file", "The file containing the topics for each paper", false)
  val keywordsFile = new CmdOption[String]("keywords-file", "The file containing the keywords for each paper", false)
}

object PopulateAuthorMentionDBFromJSON {

  def main(args: Array[String]): Unit = {
    val opts = new PopulateAuthorMentionDBFromJSONOpts()
    opts.parse(args)

    val mentions = LoadJSONAuthorMentions.loadMultiple(new File(opts.jsonFile.value),opts.codec.value,opts.numThreads.value)
    val db = new AuthorMentionDB(opts.hostname.value,opts.port.value,opts.dbname.value,opts.collectionName.value,false)

    PopulateAuthorMentionDB.insertPar(mentions,db,opts.bufferSize.value)
    db.addIndices()

  }
}


