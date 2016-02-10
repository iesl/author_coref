package edu.umass.cs.iesl.author_coref.db

import java.io.File

import edu.umass.cs.iesl.author_coref.data_structures.coreference.AuthorMention
import edu.umass.cs.iesl.author_coref.load.LoadJSONAuthorMentions
import edu.umass.cs.iesl.author_coref.utilities.{NumLines, CodecCmdOption, MongoDBOpts, NumThreads}


class PopulateAuthorMentionDBFromJSONOpts extends CodecCmdOption with NumThreads with MongoDBOpts with NumLines {
  val jsonFile = new CmdOption[List[String]]("json-file", "Either a single file (parallelized within the file), multiple files (parallelized across files), or a single directory (parallelized across files in the directory)", false)
  val bufferSize = new CmdOption[Int]("buffered-size", "The size of the buffer to use", true)
  val topicsFile = new CmdOption[String]("topics-file", "The file containing the topics for each paper", false)
  val keywordsFile = new CmdOption[String]("keywords-file", "The file containing the keywords for each paper", false)
}

object PopulateAuthorMentionDBFromJSON {

  def main(args: Array[String]): Unit = {
    val opts = new PopulateAuthorMentionDBFromJSONOpts()
    opts.parse(args)


    val inputFiles = opts.jsonFile.value.map(new File(_))

    val mentions: Iterable[Iterator[AuthorMention]] = if (inputFiles.length == 1) {
      if (inputFiles.head.isDirectory) {
        LoadJSONAuthorMentions.fromDir(inputFiles.head,opts.codec.value)
      } else {
        LoadJSONAuthorMentions.loadMultiple(inputFiles.head,opts.codec.value,opts.numThreads.value, if (opts.numLines.wasInvoked) Some(opts.numLines.value) else None)
      }
    } else {
      LoadJSONAuthorMentions.fromFiles(inputFiles,opts.codec.value)
    }
    val db = new AuthorMentionDB(opts.hostname.value,opts.port.value,opts.dbname.value,opts.collectionName.value,false)

    PopulateAuthorMentionDB.insertPar(mentions,db,opts.bufferSize.value)
    db.addIndices()

  }
}


