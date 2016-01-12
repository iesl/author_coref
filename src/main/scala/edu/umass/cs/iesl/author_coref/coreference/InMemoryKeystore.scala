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

import java.io.{BufferedReader, File, FileInputStream, InputStreamReader}
import java.util

import cc.factorie._
import edu.umass.cs.iesl.author_coref._
import edu.umass.cs.iesl.author_coref.utilities.KeystoreOpts

import scala.collection.JavaConverters._

// Modified from factorie to remove the "missing keys" feature.

trait Keystore {
  def dimensionality:Int
  def retrieve(key:String):Option[Array[Double]]

  import cc.factorie.util.VectorUtils._
  def generateVector(keys:Iterable[String]):Array[Double] = keys.flatMap{ key =>
    val res = retrieve(key)
    res
  }.foldLeft(new Array[Double](dimensionality)){case (tot, arr) => tot += arr; tot}
}

class InMemoryKeystore(map: scala.collection.Map[String,Array[Double]]) extends Keystore {
  override def dimensionality: Int = map.head._2.length

  override def retrieve(key: String): Option[Array[Double]] = map.get(key)

  def nearestNeighbors(key: String, N: Int) = {
    val top = new cc.factorie.util.TopN[String](N)
    val maybeKeyEmb = retrieve(key)
    if (maybeKeyEmb.isDefined) {
      val keyEmb = maybeKeyEmb.get
      for (entry <- map) {
        top.+=(0,entry._2.cosineSimilarity(keyEmb),entry._1)
      }
    }
    top.map(entry=> (entry.category,entry.score))
  }
}

class CaseInsensitiveInMemoryKeystore(map: scala.collection.Map[String,Array[Double]]) extends InMemoryKeystore(map.map(f => f._1.toLowerCase -> f._2)) {
  override def retrieve(key: String): Option[Array[Double]] = map.get(key.toLowerCase)
}


object InMemoryKeystore {

  def fromCmdOpts(opts: KeystoreOpts) = {
    fromFile(new File(opts.keystorePath.value),opts.keystoreDim.value,opts.keystoreDelim.value,opts.codec.value,opts.caseSensitive.value)
  }

  def fromFile(embeddingFile:File, dimensionality:Int, fileDelimiter:String, codec: String, caseSensitive: Boolean = true) = {
    val _store = new util.HashMap[String,Array[Double]](10000).asScala

    new BufferedReader(new InputStreamReader(new FileInputStream(embeddingFile),codec)).toIterator.foreach {
      line =>
        val split = line.split(fileDelimiter)
        if (split.length == dimensionality + 1) { //plus 1 b/c format is wordtoken emb
        val key :: vec = line.split(fileDelimiter).toList
          _store.put(key, vec.map(_.toDouble).toArray)
        } else {
          println(s"[${this.getClass.getSimpleName}] WARNING: error reading line: $line")
        }
    }
    if (caseSensitive) new InMemoryKeystore(_store) else new CaseInsensitiveInMemoryKeystore(_store)
  }

  def fromFileContainingDim(embeddingFile: File, fileDelimiter: String, codec: String, caseSensitive: Boolean = true) = {

    val lines = new BufferedReader(new InputStreamReader(new FileInputStream(embeddingFile),codec)).toIterator
    val Array(numItems,dimensionality) = lines.next().split(fileDelimiter).map(_.toInt)
    val _store = new util.HashMap[String,Array[Double]](numItems).asScala

    lines.foreach {
      line =>
        val split = line.split(fileDelimiter)
        if (split.length == dimensionality + 1) { //plus 1 b/c format is wordtoken emb
        val key :: vec = line.split(fileDelimiter).toList
          _store.put(key, vec.map(_.toDouble).toArray)
        } else {
          println(s"[${this.getClass.getSimpleName}] WARNING: error reading line: $line")
        }
    }
    if (caseSensitive) new InMemoryKeystore(_store) else new CaseInsensitiveInMemoryKeystore(_store)

  }
}
