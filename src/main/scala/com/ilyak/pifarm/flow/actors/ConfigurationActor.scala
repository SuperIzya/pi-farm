package com.ilyak.pifarm.flow.actors

import akka.actor.{ Actor, ActorLogging, ActorRef, Props }
import com.ilyak.pifarm.BroadcastActor.Producer
import com.ilyak.pifarm.Result
import com.ilyak.pifarm.Types.{ Result, SMap }
import com.ilyak.pifarm.common.db.Tables
import com.ilyak.pifarm.configuration.Builder
import com.ilyak.pifarm.flow.actors.SocketActor.ConfigurationFlow
import com.ilyak.pifarm.flow.configuration.{ BlockType, Configuration }
import com.ilyak.pifarm.io.http.JsContract
import com.ilyak.pifarm.plugins.PluginLocator
import play.api.libs.json._
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.JdbcProfile

import scala.concurrent.Future

class ConfigurationActor(broadcast: ActorRef)
                        (implicit db: Database,
                         profile: JdbcProfile,
                         locator: PluginLocator) extends Actor with ActorLogging {

  import ConfigurationActor._
  import context.dispatcher
  import profile.api._

  private val query = Tables.ConfigurationsTable
  var configurations: SMap[Configuration.Graph] = Map.empty

  def parse(s: String): Result[Configuration.Graph] =
    Json.fromJson[Configuration.Graph] {
      Json.parse(s)
    } match {
      case JsSuccess(v, _) => Result.Res(v)
      case JsError(e) => Result.Err(s"$e")
    }

  def load(io: DBIO[Seq[Tables.Configurations]]): Future[Unit] = {
    db.run(io)
      .map {
        _.map(c => c.name -> parse(c.graph)).toMap
      }
      .map(self ! RestoreConfigurations(_))
  }

  private def getMap[T](name: String)
                    (map: Tables.Configurations => T): Query[Tables.ConfigurationsTable, T, _] =
    for { c <- query if c.name === name } yield map(c)

  private def get(name: String) = getMap[Tables.Configurations](name)(x => x)

  broadcast ! Producer(self)
  load(query.result)

  override def receive: Receive = {
    case RestoreConfigurations(configs) =>
      configurations = configs.collect {
        case (key, Result.Res(c)) => key -> c
      }
      broadcast ! Configurations(configurations)

      configs.collect {
        case (key, Result.Err(e)) => log.error(s"Failed to restore configuration $key due to $e")
      }
    case GetConfigurations => sender() ! Configurations(configurations)
    case AddConfiguration(name, graph) =>
      Builder.test(graph) match {
        case Result.Err(e) => sender() ! GraphFaulty(graph, name, e)
        case Result.Res(_) =>
          val str = Json.asciiStringify(Json.toJson(graph))
          val config = Tables.Configurations(name, str)
          load {
            query.insertOrUpdate(config).andThen(query.result)
          }
      }
    case ChangeConfigName(oldName, newName) =>
      val action = getMap(oldName)(_.name).update(newName)
      load(action.andThen(query.result))
    case DeleteConfiguration(name) =>
      load(get(name).delete.andThen(query.result))

    case ClearAll => load(query.delete.andThen(query.result))

  }
}

object ConfigurationActor {
  def props(broadcast: ActorRef)
           (implicit db: Database, locator: PluginLocator, profile: JdbcProfile): Props =
    Props(new ConfigurationActor(broadcast))

  implicit val blockTypeFormat: OFormat[BlockType] = new OFormat[BlockType] {
    override def writes(o: BlockType): JsObject = {
      val tpe: String = o match {
        case BlockType.Automaton => "automaton"
        case BlockType.Container => "container"
      }
      Json.obj("type" -> tpe)
    }

    override def reads(json: JsValue): JsResult[BlockType] = (json \ "type").as[String] match {
      case "automation" => JsSuccess(BlockType.Automaton)
      case "container" => JsSuccess(BlockType.Container)
      case x => JsError(s"Wrong block type $x")
    }
  }
  implicit val metaFormat: OFormat[Configuration.MetaData] = Json.format
  implicit val nodeFormat: OFormat[Configuration.Node] = Json.format
  implicit val graphFormat: OFormat[Configuration.Graph] = Json.format

  case class RestoreConfigurations(configs: SMap[Result[Configuration.Graph]])

  case object GetConfigurations extends JsContract with ConfigurationFlow

  implicit val getConfigFormat: OFormat[GetConfigurations.type] = Json.format
  JsContract.add[GetConfigurations.type]("configurations-get")


  case class Configurations(list: SMap[Configuration.Graph]) extends JsContract

  implicit val configsFormat: OFormat[Configurations] = Json.format
  JsContract.add[Configurations]("configurations")

  case class AddConfiguration(name: String, graph: Configuration.Graph) extends JsContract with ConfigurationFlow

  implicit val addConfigFormat: OFormat[AddConfiguration] = Json.format
  JsContract.add[AddConfiguration]("configuration-add")

  case class ChangeConfigName(oldName: String, newName: String) extends JsContract with ConfigurationFlow

  implicit val changeConfigNameFormat: OFormat[ChangeConfigName] = Json.format
  JsContract.add[ChangeConfigName]("configuration-change-name")

  case class DeleteConfiguration(name: String) extends JsContract with ConfigurationFlow

  implicit val delConfigFormat: OFormat[DeleteConfiguration] = Json.format
  JsContract.add[DeleteConfiguration]("configuration-delete")

  case class GraphFaulty(graph: Configuration.Graph, name: String, errors: String) extends JsContract

  implicit val faultFormat: OFormat[GraphFaulty] = Json.format
  JsContract.add[GraphFaulty]("configuration-errors")

  case object ClearAll extends JsContract with ConfigurationFlow

  implicit val clearFormat: OFormat[ClearAll.type] = Json.format
  JsContract.add[ClearAll.type]("configurations-clear-all")

}

