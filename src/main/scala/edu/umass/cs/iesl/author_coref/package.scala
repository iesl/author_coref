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

package edu.umass.cs.iesl

import java.io._
import java.util
import cc.factorie._
import scala.collection.JavaConverters._

package object author_coref {

  
  implicit class IterableExtras[T](iterable: Iterable[T]) {
    
    def stableRemoveDuplicates() = {
      var uniq = new util.HashSet[T]().asScala
      val res = iterable.flatMap{
        case f =>
        if (uniq.contains(f))
          None
        else {
          uniq.add(f)
          Some(f)
        }
      }
      uniq = null
      res
    }
    
  }
  
  
  implicit class StrAddOns(string: String) {

    def noneIfEmpty = if (string.isEmpty) None else Some(string)
    def replaceTabWithSpace() = string.replaceAll("\t", " ")
    def replaceNewLineWithSpace() = string.replaceAll("\n"," ")
    def replaceTabAndNewLineWithSpace() = string.replaceTabWithSpace().replaceNewLineWithSpace()
    def trimBegEnd() = string.replaceAll("^\\s+|\\s+$","")
    def removeNewLinesAndTabs() = string.replaceAll("\n|\t","")
    def removePunctuation(replacement: String = "") = string.replaceAll("\\p{Punct}",replacement)
    
    def writeToFile(file: File, codec: String = "UTF-8") = {
      val pw = new PrintWriter(file,codec)
      pw.print(string)
      pw.close()
    }
  }
  

  implicit class ClassExtras(cls: Class[_]) {

    def ordinaryName: String = cls.getSimpleName.replaceAll("\\$$","")

  }

  implicit class FileExtras(file: File) {

    def numLines =
      new BufferedReader(new FileReader(file)).toIterator.size

    def mkParentDirs():Unit =
      file.getParentFile.toNotNull.map(_.mkdirs())
    
    def lines(codec: String = "UTF-8", start: Int = 0, end: Int = Int.MaxValue) =
      new BufferedReader(new InputStreamReader(new FileInputStream(file),codec)).toIterator.zipWithIndex.filter(p => p._2 >= start && p._2 < end).map(_._1)
    
    def getStringContents(codec: String = "UTF-8") = file.lines(codec).mkString("\n")
    
    def allFilesRecursively(filter: File => Boolean = _ => true): Iterable[File] = {
      if (file.isDirectory)
        file.listFiles().toIterable.filter(filter).filterNot(_.isDirectory) ++ file.listFiles().toIterable.filter(filter).filter(_.isDirectory).map( f => f.allFilesRecursively(filter)).flatten
      else
      if (filter(file))
        Some(file)
      else 
        None
    }
    
    def getNameWithoutExtension = file.getName.split("\\.").slice(0,1).mkString("")

  }

  implicit class ArrayDoubleExras(arr: Array[Double]) {

    def cosineSimilarity(other: Array[Double]) =
      arr.dot(other) / (arr.norm * other.norm)


    def dot(other: Array[Double]) = {
      assert(arr.length == other.length)
      var sum = 0.0
      var i = 0
      while (i < arr.length) {
        sum += arr(i) * other(i)
        i += 1
      }
      sum
    }

    def norm = {
      var sum = 0.0
      var i = 0
      while (i < arr.length) {
        sum += arr(i) * arr(i)
        i += 1
      }
      Math.sqrt(sum)
    }
  }
  
  final val InstitutionStopWords = Set("dept", "department", "departments",
    "inst", "institute", "u", "univ", "university", "universities", "coll", "college",
    "ltd", "lab", "laboratory")
  
  final val EditorStopWords = Set("eds", "ed", "editor", "editors")
  
  final val JournalStopWords = Set("proc", "proceeding", "proceedings", "international", "journal", "technical", "tech", "rep", "report", "workshop")
  
  final val KeywordStopWords = Set("keyword","keywords")

  final val EmailCharacters = "qwertyuiopasdfghjklzxcvbnm.@"

}
