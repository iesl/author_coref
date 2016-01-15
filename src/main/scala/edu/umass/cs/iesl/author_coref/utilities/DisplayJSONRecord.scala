package edu.umass.cs.iesl.author_coref.utilities

import java.io.{PrintWriter, File}

import cc.factorie.util.{JsonCubbieConverter, DefaultCmdOptions}
import edu.umass.cs.iesl.author_coref.load.LoadJSONAuthorMentions


class DisplayJSONRecordOpts extends DefaultCmdOptions with CodecCmdOption{
  val input = new CmdOption[String]("input", "The input json file", true)
  val output = new CmdOption[String]("output", "The output file", false)
  val format = new CmdOption[String]("format", "stdout", "STRING","The format of the output. Options: stdout (write only to standard out),  txt-pretty, txt-compact, html")
}


/**
  * Utility to visualize a JSON Author Mention and make sure it is being loaded sensibly.
  * Usage:
  *
  * java -Xmx1G -cp target/author_coref-1.1-SNAPSHOT-jar-with-dependencies.jar \
  *   edu.umass.cs.iesl.author_coref.utilities.DisplayJSONRecord \
  *   --input your_file.json --format format_option --output output_filename
  *
  *   The possible formats are:
  *       stdout = write the parsed json to standard out in the expanded, easy to read json format, no output file needed
  *       html = convert the json into an author mention and generate an html formatting of the record, write the result to the output file
  *       txt-pretty = write the parsed json in the expanded, easy to read json format in the output file
  *       txt-compact = write the parsed json in the compact, one record per line format to the output file
  *
  */
object DisplayJSONRecord {

  def main(args: Array[String]): Unit = {
    val opts = new DisplayJSONRecordOpts
    opts.parse(args)

    val records = LoadJSONAuthorMentions.load(new File(opts.input.value),opts.codec.value)

    opts.format.value match {
      case "stdout" =>
        import org.json4s.jackson.JsonMethods._
        val strings = records.map(f => pretty(render(JsonCubbieConverter.toJson(f))))
        strings.foreach(println)
      case "txt-pretty" =>
        val pw = new PrintWriter(opts.output.value,opts.codec.value)
        import org.json4s.jackson.JsonMethods._
        val strings = records.map(f => pretty(render(JsonCubbieConverter.toJson(f))))
        strings.foreach(pw.println)
        pw.close()
      case "txt-compact" =>
        val pw = new PrintWriter(opts.output.value,opts.codec.value)
        import org.json4s.jackson.JsonMethods._
        val strings = records.map(f => compact(render(JsonCubbieConverter.toJson(f))))
        strings.foreach(pw.println)
        pw.close()
      case "html" =>
        val pw = new PrintWriter(opts.output.value,opts.codec.value)
        val strings = records.map(_.toHTMLFormattedString)
        strings.foreach(pw.println)
        pw.close()
    }

  }

}
