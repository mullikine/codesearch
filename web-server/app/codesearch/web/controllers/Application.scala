package codesearch.web.controllers

import codesearch.core.index.{CratesIndex, GemIndex, HackageIndex, NpmIndex}
import com.github.marlonlom.utilities.timeago.TimeAgo
import javax.inject.Inject
import play.api.mvc.InjectedController

import scala.concurrent.ExecutionContext

case class LangInfo(updatedMills: Long, totalPackages: Int) {
  val updatedAgo: String = TimeAgo.using(updatedMills)
}

class Application @Inject() (
  implicit val executionContext: ExecutionContext
) extends InjectedController {

  def haskell = Action.async { implicit request =>
    HackageIndex.updated.map(updated => Ok(views.html.haskell(
      TimeAgo.using(updated.getTime)
    )))
  }

  def rust = Action.async { implicit request =>
    CratesIndex.updated.map(updated => Ok(views.html.rust(
      TimeAgo.using(updated.getTime)
    )))
  }

  def javascript = Action.async { implicit request =>
    NpmIndex.updated.map(updated => Ok(views.html.javascript(
      TimeAgo.using(updated.getTime)
    )))
  }

  def ruby = Action.async { implicit request =>
    GemIndex.updated.map(updated => Ok(views.html.ruby(
      TimeAgo.using(updated.getTime)
    )))

  }

  def index = Action.async { implicit request =>
    HackageIndex.updated.zip(HackageIndex.getSize)
      .zip(CratesIndex.updated.zip(CratesIndex.getSize))
      .zip(NpmIndex.updated.zip(NpmIndex.getSize))
      .zip(GemIndex.updated.zip(GemIndex.getSize))
      .map {
      case ((((updatedHackage, sizeHackage), (updatedCrates, sizeCrates)), (updatedNpm, sizeNpm)), (updatedGem, sizeGem)) =>
        Ok(views.html.index(
          LangInfo(updatedHackage.getTime, sizeHackage),
          LangInfo(updatedCrates.getTime, sizeCrates),
          LangInfo(updatedGem.getTime, sizeGem),
          LangInfo(updatedNpm.getTime, sizeNpm)
        ))
    }
  }

  def untrail(path: String) = Action {
    MovedPermanently("/" + path)
  }
}
