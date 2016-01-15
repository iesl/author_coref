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

import cc.factorie.app.bib.hcoref.InMemoryHashMapKeystoreOpts
import cc.factorie.util.DefaultCmdOptions


trait CodecCmdOption extends DefaultCmdOptions {
  val codec = new CmdOption[String]("codec", "UTF-8", "STRING", "The file encoding to use.")
}

trait NumThreads extends DefaultCmdOptions {
  val numThreads = new CmdOption[Int]("num-threads", 1, "INT", "The number of threads to use.")
}


/**
 * The command line options used for any connection to mongo.
 */
trait MongoDBOpts extends DefaultCmdOptions{
  val hostname = new CmdOption[String]("hostname", "The host of the mongo server", true)
  val port = new CmdOption[Int]("port", "The port of the mongo server", true)
  val dbname = new CmdOption[String]("dbname", "The name of the database", true)
  val collectionName = new CmdOption[String]("collection-name", "The name of the collection", true)
}

trait KeystoreOpts extends InMemoryHashMapKeystoreOpts with CodecCmdOption {
  val caseSensitive = new CmdOption[Boolean]("case-sensitive", true, "BOOLEAN", "Whether or not to make the keystore case sensitive")
}

trait CanopyOpts extends DefaultCmdOptions {
  val canopies = new CmdOption[List[String]]("canopies",List("fullName","firstAndLast","lastAndFirst3ofFirst", "lastAndFirst1ofFirst"),"STRINGS", "The blocking rules/canopies to use in order. Specified as strings. See canopy documentation for more information.")
}

trait NameProcessorOpts extends DefaultCmdOptions {
  val nameProcessor = new CmdOption[String]("name-processor", "CaseInsensitiveReEvaluatingNameProcessor","STRING", "The name processor to use in when handling author mentions.")
}