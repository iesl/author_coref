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

import java.io.{BufferedReader, File, FileInputStream, InputStreamReader}

import cc.factorie._
import edu.umass.cs.iesl.author_coref._
import edu.umass.cs.iesl.author_coref.data_structures.{Author, RawRexaAuthorMention, RexaAuthorMention}

import scala.collection.mutable.ArrayBuffer

object LoadRexa {

  def fromDir(rexaRoot: File, codec: String) = {
    val canopies = rexaRoot.listFiles().filter(_.isDirectory)
    canopies.flatMap {
      canopyDir =>
        val canopyName = canopyDir.getName
        println(s"[LoadRexa] Loading from canopy $canopyName")
        val entities = canopyDir.listFiles().filter(_.isDirectory)
        entities.flatMap {
          entityDir =>
            val entitySlug = entityDir.getName
            println(s"[LoadRexa] Loading mentions for the entity $entitySlug")
            val entityFiles = entityDir.listFiles()
            entityFiles.map(fromFile(canopyName, Some(entitySlug), _, codec))
        }
    }
  }

  def fromFile(canopy: String, groundTruth: Option[String], file: File, codec: String = "UTF-8"): RexaAuthorMention = {
    fromStrings(file.getName, canopy, groundTruth, new BufferedReader(new InputStreamReader(new FileInputStream(file), codec)).toIterator.toIterable)
  }

  private val nameGetter = AlmostXMLTag("n")
  private val firstNameGetter = AlmostXMLTag("f")
  private val middleNameGetter = AlmostXMLTag("m")
  private val lastNameGettter = AlmostXMLTag("l")

  def getAuthor(string: String): Author = {
    val firsts = firstNameGetter.getValues(string)
    val middle = middleNameGetter.getValues(string)
    val lasts = lastNameGettter.getValues(string)

    val middleNames = new ArrayBuffer[String]()

    val firstName = if (firsts.size == 0) {
      println(s"[RexaMentionEntry] WARNING: Author string $string. Has no first name.")
      None
    }
    else if (firsts.size > 1) {
      println(s"[RexaMentionEntry] WARNING: The author string $string has more than one first name. The name appearing closest to the start of the string will be selected and the others considered middle names.")
      middleNames ++= firsts.drop(1)
      firsts.headOption
    } else {
      firsts.headOption
    }

    val lastName = if (lasts.size == 0) {
      println(s"[RexaMentionEntry] WARNING: Author string $string. Has no last name.")
      None
    }
    else if (firsts.size > 1) {
      println(s"[RexaMentionEntry] WARNING: The author string $string has more than one last name. The name appearing closest to the start of the string will be selected.")
      lasts.headOption
    } else {
      lasts.headOption
    }

    new Author(firstName, middleNames, lastName)
  }

  def getAuthors(string: String) = {
    nameGetter.getValues(string).map(getAuthor)
  }


  def fromStrings(id: String, canopy: String, groundTruth: Option[String], entries: Iterable[String]) = {
    var abstractText: Option[String] = None
    val alt_authorlist = new ArrayBuffer[Author]()
    var altTitle: Option[String] = None
    var author_in_focus: Option[Author] = None
    var author_in_focus_score: Option[Double] = None
    val authorlist = new ArrayBuffer[Author]()
    var body: Option[String] = None
    var editor: Option[String] = None
    val emails = new ArrayBuffer[String]()
    val institutions = new ArrayBuffer[String]()
    var venue: Option[String] = None
    var journal: Option[String] = None
    var keyword: Option[String] = None
    var title: Option[String] = None
    var year: Option[String] = None

    
    // what we should do to handle the fields that appear more than once is load them all in
    // and then leave it up to the next steps to remove duplicates.
    
    entries.filter(_.nonEmpty).foreach {
      entry =>
        val split = entry.split(":|=")
        val field = split(0)
        val value = split.drop(1).mkString(":").trimBegEnd() // TODO: Hopefully this is fine
        field match {
          case "abstract" =>
            abstractText = Some(value)

          case "alt-authorlist" =>
            alt_authorlist ++= getAuthors(value)

          case "altTitle" =>
            altTitle = Some(value)

          case "author-in-focus" =>
            author_in_focus = Some(getAuthor(value))

          case "author-in-focus-score" =>
            author_in_focus_score = value.toDoubleSafe

          case "authorlist" =>
            authorlist ++= getAuthors(value)

          case "body" =>
            body = Some(value)

          case "editor" =>
            editor = Some(value)

          case "email" =>
            emails += value

          case "institution" =>
            institutions += value

          case "venue" =>
            venue = Some(value)

          case "journal" =>
            journal = Some(value)

          case "keyword" =>
            keyword = Some(value.replaceAll("(?i)keywords:",""))

          case "title" =>
            title = Some(value)

          case "year" =>
            year = Some(value)
          case _ =>
            println(s"[RexaMentionEntry] Malformed line: $entry")
        }
    }
    RawRexaAuthorMention(id, canopy, groundTruth, abstractText, alt_authorlist, altTitle, author_in_focus, author_in_focus_score, authorlist, body, editor, emails, institutions, venue, journal, keyword, title, year).toRexaAuthorMention
  }

}


object LoadTest {


  def main(args: Array[String]): Unit = {

    val mentions = LoadRexa.fromDir(new File(args(0)), "UTF-8")
    println(mentions(0))
    println(mentions(1))
  }

}

