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

package edu.umass.cs.iesl.author_coref.load

import java.io.File
import edu.umass.cs.iesl.author_coref._


object LoadTabSeparatedTuples {
  def load(file: File, codec: String):Iterable[(String,String)] = loadLines(file.lines(codec).toIterable)

  def loadLines(lines: Iterable[String]) = lines.map(loadLine)

  def loadLine(line: String) = {
    val split = line.split("\t")
    assert(split.length == 2, s"Malformed line: $line")
    (split(0),split(1))
  }
}
