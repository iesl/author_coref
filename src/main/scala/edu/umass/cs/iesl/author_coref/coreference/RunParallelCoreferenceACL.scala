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

package edu.umass.cs.iesl.author_coref.coreference

import java.io.{File, PrintWriter}

import edu.umass.cs.iesl.author_coref.data_structures.coreference.AuthorMention
import edu.umass.cs.iesl.author_coref.db.AuthorMentionDB
import edu.umass.cs.iesl.author_coref.load.LoadCorefTasks

/**
  * This was used in running coreference on ACL papers. Another example of running using multiple threads and a mongodb
  * backend database.
  */
object RunParallelCoreferenceACL {

  def main(args: Array[String]): Unit = {

    // Uses command line options from factorie
    val opts = new RunParallelOpts
    opts.parse(args)

    // Load all of the coref tasks into memory, so they can easily be distributed amongst the different threads
    val allWork = LoadCorefTasks.load(new File(opts.corefTaskFile.value),opts.codec.value)

    // Create the interface to the MongoDB containing the mentions
    val db = new AuthorMentionDB(opts.hostname.value, opts.port.value, opts.dbname.value, opts.collectionName.value, false)

    // The lookup table containing the embeddings. 
    val keystore = InMemoryKeystore.fromCmdOpts(opts)

    // Create the output directory
    new File(opts.outputDir.value).mkdirs()

    // Canopy Functions
    //val canopyFunctions = Iterable((a:AuthorMention) => Canopies.fullName(a.self.value),(a:AuthorMention) => Canopies.firstAndLast(a.self.value), (a:AuthorMention) => Canopies.lastAndFirstNofFirst(a.self.value,3))
    val canopyFunctions = Iterable((a:AuthorMention) => Canopies.fullName(a.self.value),(a:AuthorMention) => Canopies.firstAndLast(a.self.value), (a:AuthorMention) => Canopies.lastAndFirstNofFirst(a.self.value,3), (a:AuthorMention) => Canopies.lastAndFirstNofFirst(a.self.value,1))

    // Initialize the coreference algorithm
    val parCoref = new ParallelHierarchicalCoref(allWork,db,opts,keystore,canopyFunctions,new File(opts.outputDir.value))

    // Run the algorithm on all the tasks
    parCoref.runInParallel(opts.numThreads.value)

    // Write the timing info
    val timesPW = new PrintWriter(new File(opts.outputDir.value,"timing.txt"))
    timesPW.println(parCoref.times.map(f => f._1 + "\t" + f._2).mkString("\n"))
    timesPW.close()

    // display the timing info
    parCoref.printTimes()
  }
}