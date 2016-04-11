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

import cc.factorie.app.nlp.hcoref._
import cc.factorie.infer.SettingsSampler

// Modification from DefaultMoveGenerator, such that merge lefts can only be performed with entity nodes.

trait AuthorCorefMoveGenerator[Vars <: NodeVariables[Vars]]  extends MoveGenerator[Vars]{
  this :SettingsSampler[(Node[Vars], Node[Vars])] =>

  def settings(c:(Node[Vars], Node[Vars])) = new MoveSettingIterator[Vars] {
    var (e1, e2) = c

    val moves = new scala.collection.mutable.ArrayBuffer[Move[Vars]]()

    if(e1.root != e2.root) {
      if(e1.isRoot && e2.isRoot) {
        moves += new MergeUp[Vars](e1, e2)({d => newInstance(d)})
      } else {
        while (e1 != null) {
          if(e1.mentionCountVar.value >= e2.mentionCountVar.value) {
            if (!e1.isMention)
              moves += new MergeLeft[Vars](e1, e2)
          } else {
            if (!e2.isMention)
              moves += new MergeLeft[Vars](e2, e1)
          }
          e1 = e1.getParent.getOrElse(null.asInstanceOf[Node[Vars]])
        }
      }
    } else {
      if (!e1.isMention && !e2.isMention)
        if(e1.mentionCountVar.value > e2.mentionCountVar.value) {
          moves += new SplitRight[Vars](e2, e1)
        } else {
          moves += new SplitRight[Vars](e1, e2)
        }
    }

    moves += new NoMove[Vars]
  }
}