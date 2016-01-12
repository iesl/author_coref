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

package edu.umass.cs.iesl.author_coref.process

import java.text.Normalizer

import edu.umass.cs.iesl.author_coref._
import edu.umass.cs.iesl.author_coref.data_structures.PersonName

/**
 * This file contains a variety of name processing steps
 */

trait NameProcessor {
  def process(name: PersonName): PersonName
}

class CompoundNameProcessor(processors: Iterable[NameProcessor]) extends NameProcessor {
  override def process(name: PersonName): PersonName = {
    var res: PersonName = null
    processors.foreach(p => res = p.process(name))
    res
  }
}

object IdentityNameProcessor extends NameProcessor{
  def process(name: PersonName): PersonName = name
}

object FirstNameTrimmer extends NameProcessor {
  override def process(name: PersonName): PersonName = {
    name.firstName.opt.foreach(n => name.firstName.set(n.trimBegEnd()))
    name
  }
}

object LastNameTrimmer extends NameProcessor {
  override def process(name: PersonName): PersonName = {
    name.lastName.opt.foreach(n => name.lastName.set(n.trimBegEnd()))
    name
  }
}

object FirstMiddleSplitter extends NameProcessor {
  override def process(name: PersonName): PersonName = {
    val split = name.firstName.opt.map(_.split("\\s")).getOrElse(Array())
    name.firstName.set(split.headOption)
    name.middleNames.set(name.middleNames.opt.getOrElse(Seq()) ++ split.drop(1))
    name
  }
}

object LastNameSuffixProcessor extends NameProcessor {
  override def process(name: PersonName): PersonName = {
    val (newLastname, newSuffixes) = processLastName(name.lastName.opt.getOrElse(""))
    name.lastName.set(newLastname)
    name.suffixes.set(name.suffixes.opt.getOrElse(Seq()) ++ newSuffixes)
    name
  }

  def processLastName(string: String) = {
    val splitOnComma = string.split(",").map(_.trimBegEnd())
    if (splitOnComma.isEmpty)
      println("WARNING: name is a single comma.")
    val name = splitOnComma.headOption.getOrElse("")
    val suffixes = splitOnComma.drop(1)
    (name,suffixes)
  }
}

trait AccentNameProcessor extends NameProcessor {

  val accentRegex = "\\{[a-zA-Z0-9]+ over \\(([a-zA-Z0-9\\s]+)\\)\\}".r

  def processString(stringName: String) = {
    accentRegex.replaceAllIn(stringName,regexMatch => regexMatch.group(1))
  }

}

object LastNameAccentNameProcessor extends AccentNameProcessor {
  override def process(name: PersonName): PersonName =
    name.lastName.set(name.lastName.opt.map(processString))
}


object FirstNameAccentNameProcessor extends AccentNameProcessor {
  override def process(name: PersonName): PersonName =
    name.firstName.set(name.firstName.opt.map(processString))
}

trait UnicodeCharacterAccentNameProcessor extends NameProcessor {
  def processString(stringName: String)= {
    Normalizer.normalize(stringName, Normalizer.Form.NFKD).replaceAll("[^\\p{ASCII}]", "")
  }
}

object LastNameUnicodeCharacterAccentNameProcessor extends UnicodeCharacterAccentNameProcessor {
  override def process(name: PersonName): PersonName =
    name.lastName.set(name.lastName.opt.map(processString))
}

object FirstNameUnicodeCharacterAccentNameProcessor extends UnicodeCharacterAccentNameProcessor {
  override def process(name: PersonName): PersonName =
    name.firstName.set(name.firstName.opt.map(processString))
}


trait AlphabeticOnlyNameProcessor extends NameProcessor {
  val regex = "[^a-zA-Z]"
  def processString(stringName: String) = {
    stringName.replaceAll(regex,"")
  }
}

object AlphabeticOnlyLastNameProcessor extends AlphabeticOnlyNameProcessor {
  override def process(name: PersonName): PersonName =
    name.lastName.set(name.lastName.opt.map(processString))
}

object AlphabeticOnlyMiddleNameProcessor extends AlphabeticOnlyNameProcessor {
  override def process(name: PersonName): PersonName =
    name.middleNames.set(name.middleNames.opt.map(c => c.map(processString)))
}

object AlphabeticOnlySuffixesProcessor extends AlphabeticOnlyNameProcessor {
  override def process(name: PersonName): PersonName =
    name.suffixes.set(name.suffixes.opt.map(c => c.map(processString)))
}

object AlphabeticOnlyFirstNameProcessor extends AlphabeticOnlyNameProcessor {
  override def process(name: PersonName): PersonName =
    name.firstName.set(name.firstName.opt.map(processString))
}

object AlphabeticOnlyAllNamesProcessor extends CompoundNameProcessor(Iterable(AlphabeticOnlyFirstNameProcessor,AlphabeticOnlyMiddleNameProcessor,AlphabeticOnlyLastNameProcessor,AlphabeticOnlySuffixesProcessor))

object LowerCaseFirstName extends NameProcessor {
  override def process(name: PersonName): PersonName = name.firstName.set(name.firstName.opt.map(_.toLowerCase))
}

object LowerCaseLastName extends NameProcessor {
  override def process(name: PersonName): PersonName = name.lastName.set(name.lastName.opt.map(_.toLowerCase))
}

object LowerCaseMiddleNames extends NameProcessor {
  override def process(name: PersonName): PersonName = name.middleNames.set(name.middleNames.opt.map(_.map(_.toLowerCase)))
}

object LowerCaseSuffixes extends NameProcessor {
  override def process(name: PersonName): PersonName = name.suffixes.set(name.suffixes.opt.map(_.map(_.toLowerCase)))
}

object LowerCaseAllNames extends CompoundNameProcessor(Iterable(LowerCaseFirstName,LowerCaseMiddleNames,LowerCaseLastName,LowerCaseSuffixes))

object ReEvaluatingNameProcessor extends CompoundNameProcessor(Iterable(FirstNameTrimmer,LastNameTrimmer,LastNameSuffixProcessor,LastNameAccentNameProcessor,FirstNameAccentNameProcessor,FirstMiddleSplitter))

object CaseInsensitiveReEvaluatingNameProcessor extends CompoundNameProcessor(Iterable(FirstNameTrimmer,LastNameTrimmer,LastNameSuffixProcessor,LastNameAccentNameProcessor,FirstNameAccentNameProcessor,FirstNameUnicodeCharacterAccentNameProcessor,LastNameUnicodeCharacterAccentNameProcessor,FirstMiddleSplitter,AlphabeticOnlyAllNamesProcessor,LowerCaseAllNames))

/**
  * Object to retrieve a name processor based on a string
  */
object NameProcessor {
  /**
    * Return the name processor associated with each string name. Matching is
    * case insensitive and ignores the "nameprocessor" part of the name
    *
    *   1) "identity" - a name processor which does nothing at all
    *   2) "CaseInsensitiveReEvaluatingNameProcessor" - normalizes names and might change middle & last names according to punctuation/spacing
    *   3) "LowerCaseAllNames" - lowercases the first, middle and last names
    *   4) "ReEvaluatingNameProcessor" - normalizes accent characters but keeps names case sensitive. might change middle & last names according to punctuation/spacing
    * @param string
    */
  def fromString(string: String):NameProcessor = string.toLowerCase.replaceAll("nameprocessor","") match {
    case "identity" => IdentityNameProcessor
    case "caseinsensitivereevaluating" => CaseInsensitiveReEvaluatingNameProcessor
    case "lowercaseallnames" => LowerCaseAllNames
    case "reevaluating" => ReEvaluatingNameProcessor
  }
}