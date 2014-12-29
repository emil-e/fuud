package com.github.fuud

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import spray.json._

import scala.collection.JavaConversions._
import scala.collection.immutable._
import scala.collection.mutable
import scala.io.Codec
import scala.sys.process._

case class IngredientSpec(ingredient: String)

case class Section[A](title: Option[String], content: A) {
  def map[B](func: A => B) = Section(title, func(content))
}

object Recipe {
  type Ingredients = Seq[Section[Seq[IngredientSpec]]]
  type Instructions = Seq[Section[Seq[String]]]

  trait JsonProtocol extends DefaultJsonProtocol {
    implicit val ingredientSpecFormat = jsonFormat1(IngredientSpec)
    implicit def sectionFormat[A : JsonFormat] = jsonFormat2(Section[A])

    implicit object RecipeWriter extends JsonWriter[Recipe] {
      override def write(recipe: Recipe): JsValue =
        JsObject(
          "title" -> recipe.title.toJson,
          "description" -> recipe.description.toJson,
          "ingredients" -> recipe.ingredients.toJson,
          "instructions" -> recipe.instructions.toJson)
    }
  }

  private case class RawSection(title: String,
                                content: Seq[Element],
                                children: Seq[RawSection]) {

    def find(predicate: RawSection => Boolean): Option[RawSection] = {
      val queue = mutable.Queue(this)
      while (queue.nonEmpty) {
        val section = queue.dequeue()
        if (predicate(section))
          return Some(section)

        queue.enqueue(section.children: _*)
      }

      None
    }

    def simplified: RawSection =
      if (title.isEmpty && content.isEmpty && (children.size == 1))
        children.head.simplified
      else
        RawSection(title, content, children.map(_.simplified))
  }

  private def markdown(md: String): String = {
    val buffer = new StringBuilder
    val logger = ProcessLogger(buffer ++= _, _ => ())
    val io = BasicIO(withIn = false, logger).withInput({ input =>
      input.write(Codec.toUTF8(md))
      input.close()
    })

    "markdown".run(io).exitValue()
    buffer.toString
  }

  private def sectionize[A, B](items: collection.Seq[A], headingOf: A => Option[B]): Seq[(B, Seq[A])] = {
    if (items.isEmpty)
      return Seq.empty

    val heading = headingOf(items.head).get
    val (left, right) = items.tail.span(headingOf(_).isEmpty)
    (heading, Seq(left: _*)) +: sectionize(right, headingOf)
  }

  private def buildStructure(level: Int, title: String, elements: collection.Seq[Element]): RawSection = {
    val isHeading: Element => Boolean = _.tagName == "h" + level
    val headingOf: Element => Option[String] = e => if (isHeading(e)) Some(e.html) else None
    val (content, sections) = elements.span(!isHeading(_))
    RawSection(
      title,
      Seq(content: _*),
      sectionize(sections, headingOf) map { case (t, c) => buildStructure(level + 1, t, c) })
  }

  def sectionsOf(root: RawSection, pred: RawSection => Boolean): Seq[Section[Seq[Element]]] = {
    root.find(pred).map { section =>
      val subSections =
        if (section.children.isEmpty)
          Seq(section)
        else
          section.children

      subSections.map(s => Section(Some(s.title).filter(_.nonEmpty), s.content))
    } getOrElse Seq.empty
  }

  def makeIngredients(root: RawSection): Recipe.Ingredients = {
    sectionsOf(root, _.title == "Ingredienser").map {
      _.map { content =>
        for {
          contentElement <- content
          listItem <- contentElement.select("ul > li, ol > li")
        } yield(IngredientSpec(listItem.html))
      }
    }
  }

  def makeInstructions(root: RawSection): Recipe.Instructions = {
    sectionsOf(root, _.title == "Instruktioner").map {
      _.map { content =>
        for {
          contentElement <- content
          listItem <- contentElement.select("ul > li, ol > li")
        } yield(listItem.html)
      }
    }
  }

  def apply(str: String): Recipe = {
    val doc = Jsoup.parse(markdown(str))
    val root = buildStructure(1, doc.title, doc.body.children).simplified
    new Recipe(
      root.title,
      root.content.foldLeft("")(_ + _.html),
      makeInstructions(root),
      makeIngredients(root))
  }
}

class Recipe(val title: String,
             val description: String,
             val instructions: Recipe.Instructions,
             val ingredients: Recipe.Ingredients) {

  override def toString = {
    val buf = new StringBuilder
    buf ++= s"# $title\n"
    buf ++= s"$description\n"

    buf ++= "## Ingredienser\n"
    for (section <- ingredients) {
      buf ++= s"### ${section.title getOrElse "Alla"}\n"
      for (spec <- section.content)
        buf ++= s"- ${spec.ingredient}\n"
      buf += '\n'
    }

    buf ++= "## Instruktioner\n"
    for (section <- instructions) {
      buf ++= s"### ${section.title getOrElse "Alla"}\n"
      for ((instruction, i) <- section.content.zipWithIndex)
        buf ++= s"${i + 1}. $instruction\n"
      buf += '\n'
    }

    buf.toString
  }
}
