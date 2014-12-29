package com.github.fuud

import java.io.IOException
import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes

import akka.actor.Actor
import akka.event.Logging

import scala.collection.immutable._
import scala.collection.mutable
import scala.util.Try
import scala.io.Source
import scala.collection.JavaConversions._

case class RecipeInfo(id: String, title: String)

object RecipeService {
  case class GetRecipeIndex()
  case class GetRecipe(id: String)
}

class RecipeService extends Actor {
  val Suffix = ".md"
  val Root = Paths.get("/Users/shadewind/Dropbox/Recipes")
  val log = Logging(context.system, this)

  def receive = {
    case RecipeService.GetRecipeIndex() => {
      log.info("Get recipe index")
      sender ! index
    }
    case RecipeService.GetRecipe(id) => {
      log.info("Get recipe {}", id)
      sender ! {
        val path = Root.resolve(id + Suffix)
        if (path.exists(_ == ".."))
          None
        else
          loadRecipe(path).toOption
      }
    }
  }

  def index: Folder[String, RecipeInfo] = {
    val tree = listFiles(Root).filter(_.getFileName.toString.endsWith(Suffix))
    // TODO log errors here
    tree.flatMap(indexRecipe(_).toOption).mapKeys(_.getFileName.toString)
  }

  def indexRecipe(path: Path): Try[RecipeInfo] =
    for {
      recipe <- loadRecipe(path)
    } yield {
      RecipeInfo(
        Root.relativize(path).toString.stripSuffix(Suffix),
        recipe.title)
    }

  def loadRecipe(path: Path): Try[Recipe] = Try {
    Recipe(Source.fromFile(path.toString).mkString)
  }

  def listFiles(dir: Path): Folder[Path, Path] = {
    class FolderVisitor extends FileVisitor[Path] {
      private class FolderBuilder(path: Path) {
        val items = new VectorBuilder[Path]
        val subFolders = new VectorBuilder[Folder[Path, Path]]

        def result: Folder[Path, Path] = Folder(path, items.result, subFolders.result)
      }

      private val builders = mutable.Stack(new FolderBuilder(dir.getParent))

      override def preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult = {
        builders.push(new FolderBuilder(dir))
        FileVisitResult.CONTINUE
      }

      override def visitFileFailed(file: Path, exc: IOException): FileVisitResult = {
        // TODO do something
        FileVisitResult.CONTINUE
      }

      override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
        builders.top.items += file
        FileVisitResult.CONTINUE
      }

      override def postVisitDirectory(dir: Path, exc: IOException): FileVisitResult = {
        val builder = builders.pop()
        builders.top.subFolders += builder.result
        FileVisitResult.CONTINUE
      }

      def result = builders.top.result
    }

    val visitor = new FolderVisitor
    Files.walkFileTree(dir, visitor)
    visitor.result.subFolders.head
  }
}
