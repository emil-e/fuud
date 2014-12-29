package com.github.fuud

import java.net.URLDecoder

import akka.actor.{Actor, Props}
import akka.event.Logging
import akka.pattern.ask
import akka.util.Timeout
import spray.http.{HttpRequest, HttpResponse}
import spray.http.MediaTypes._
import spray.json._
import spray.routing.HttpService
import spray.routing.directives.LogEntry
import spray.util.LoggingContext

import scala.concurrent.duration._

object FuudJsonProtocol extends DefaultJsonProtocol with Recipe.JsonProtocol {
  implicit val recipeInfoFormat = jsonFormat2(RecipeInfo)
}

class FuudService extends Actor with HttpService {
  import context.dispatcher

  def actorRefFactory = context
  implicit val defaultTimeout = Timeout(3.seconds)
  implicit val log = Logging(context.system, this)
  implicit val loggingContext = LoggingContext.fromAdapter(log)
  val recipeService = context.actorOf(Props[RecipeService])

  def receive = runRoute {
    logRequest(showRequest _) {
      // Default to API
      apiRoute ~
      // Otherwise, try public files
      getFromResourceDirectory("public") ~
      // But default to index page to enable client side routing
      getFromResource("public/index.html")
    }
  }

  private def showRequest(request: HttpRequest) =
    LogEntry(s"${request.method} ${request.uri.path}", Logging.InfoLevel)

  private val apiRoute = (pathPrefix("api") &  respondWithMediaType(`application/json`)) {
    import com.github.fuud.FuudJsonProtocol._

    pathPrefix("recipes") {
      (get & path(Rest)) { id =>
        complete {
          val result = recipeService ? RecipeService.GetRecipe(URLDecoder.decode(id, "UTF-8"))
          result.map { obj =>
            val optionRecipe = obj.asInstanceOf[Option[Recipe]]
            optionRecipe.map { recipe =>
              HttpResponse(entity = recipe.toJson.compactPrint)
            } getOrElse {
              HttpResponse(status = 404)
            }
          }
        }
      } ~
      (get & pathEnd) {
        complete {
          val result = recipeService ? RecipeService.GetRecipeIndex()
          result.map { index =>
            indexToJson(index.asInstanceOf[Folder[String, RecipeInfo]]).compactPrint
          }
        }
      }
    }
  }

  private def indexToJson(index: Folder[String, RecipeInfo]): JsObject = {
    import com.github.fuud.FuudJsonProtocol._

    JsObject(
      "name" -> index.key.toJson,
      "recipes" -> index.items.toJson,
      "subFolders" -> index.subFolders.map(indexToJson).toJson)
  }
}
