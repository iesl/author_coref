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

package edu.umass.cs.iesl.author_coref.db

import edu.umass.cs.iesl.author_coref.data_structures.coreference.AuthorMention

object PopulateAuthorMentionDB {
  
  def insert(mentions: Iterator[AuthorMention], db: AuthorMentionDB, bufferSize: Int) =
    insertPar(Iterable(mentions),db,bufferSize)
  
  def insertPar(mentionStreams: Iterable[Iterator[AuthorMention]], db: AuthorMentionDB, bufferSize: Int) =
    mentionStreams.par.foreach(db.bufferedInsert(_,bufferSize))
  
}




class AuthorMentionDB(override val hostname: String,
                        override val  port: Int,
                        override val  dbname: String,
                        override val  collectionName: String,
                        override val enforceIndices: Boolean)  extends MongoDatastore[AuthorMention] with Datastore[String,AuthorMention] {


  override def constructor(): AuthorMention = new AuthorMention()

  override def indices(cubbie: AuthorMention): Seq[Seq[AuthorMention#AbstractSlot[Any]]] = Seq(Seq(cubbie.mentionId))

  override def get(key: String): Iterable[AuthorMention] = db.query(q => q.mentionId.set(key)).limit(1).toIterable

}