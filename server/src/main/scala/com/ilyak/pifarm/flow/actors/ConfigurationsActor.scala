package com.ilyak.pifarm.flow.actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.stream.Materializer
import com.ilyak.pifarm.BroadcastActor.Producer
import com.ilyak.pifarm.Types.{MapGroup, Result, SMap}
import com.ilyak.pifarm.common.db.Tables
import com.ilyak.pifarm.configuration.Builder
import com.ilyak.pifarm.flow.actors.ConfigurableDeviceActor.{AllConfigs, AssignConfig, GetAllConfigs}
import com.ilyak.pifarm.flow.actors.SocketActor.{ConfigurationFlow, SocketActors}
import com.ilyak.pifarm.flow.configuration.ConfigurableNode.XLet
import com.ilyak.pifarm.flow.configuration.{BlockDescription, BlockType, Configuration}
import com.ilyak.pifarm.plugins.PluginLocator
import com.ilyak.pifarm.{JsContract, Result}
import play.api.libs.json._
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.JdbcProfile
import zio.{Ref, Task, UIO}
import com.ilyak.pifarm.dao.ZioDb._
import zio.internal.Platform

class ConfigurationsActor(broadcast: ActorRef,
                          driver: ActorRef,
                          socket: SocketActors)
                         (implicit db: Database,
                          profile: JdbcProfile,
                          locator: PluginLocator) extends Actor with ActorLogging {
  log.debug("Starting...")

  import ConfigurationsActor._
  import context.dispatcher
  import profile.api._
  val zioRuntime = zio.Runtime(context.dispatcher, Platform.default)

  private lazy val query = Tables.ConfigurationsTable
  val configurationsRef: Ref[SMap[Configuration.Graph]] = zioRuntime.unsafeRun(Ref.make(Map.empty))
  val getConfigurations: UIO[SMap[Configuration.Graph]] = for { conf <- configurationsRef.get } yield conf

  def parse(s: String): Result[Configuration.Graph] =
    Json.fromJson[Configuration.Graph] {
      Json.parse(s)
    } match {
      case JsSuccess(v, _) => Result.Res(v)
      case JsError(e) => Result.Err(s"$e")
    }

  def setConfigurations(configs: SMap[Result[Configuration.Graph]]): UIO[Unit] = for {
    conf <- configurationsRef.updateAndGet(_ ++ configs.collect {
      case (key, Result.Res(c)) => key -> c
    })
    _ <- UIO.effectTotal {
      configs.collect {
        case (key, Result.Err(e)) => log.error(s"Failed to restore configuration $key due to $e")
      }
    }
  } yield broadcast ! Configurations(conf)

  def load[T](io: DBIO[T]): Task[Unit] = for {
    res <- (io andThen query.result).toZio
    conf <- Task.effectTotal{ res.map(c => c.name -> parse(c.graph)).toMap }
    _ <- setConfigurations(conf)
  } yield ()

  private def getConfig(name: String): Query[Tables.ConfigurationsTable, Tables.Configurations, Seq] =
    for {c <- query if c.name === name} yield c

  broadcast ! Producer(self)
  load(query.result)
  val deviceActor = context.actorOf(ConfigurableDeviceActor.props(socket, driver, broadcast), "configurable-devices")

  log.debug("All initial messages are sent")

  override def receive: Receive = {
    case c: AllConfigs => socket.actor ! c
    case GetAllConfigs => deviceActor forward GetAllConfigs
    case GetConfigurationNodes =>
      sender() ! ConfigurationNodes(locator.listAll)
    case Configurations(c) =>
      runZIO {
        for {
          configurations <- configurationsRef.updateAndGet(_ ++ c)
        } yield broadcast ! Configurations(configurations)
      }
    case RestoreConfigurations(configs) =>
      runZIO { setConfigurations(configs) }
    case GetConfigurations =>
      val currSender = sender()
      runZIO {
        for {
          config <- getConfigurations
          _ <- UIO.effectTotal {
            log.debug(s"Returning configurations upon request ($config)")
          }
        } yield currSender ! Configurations(config)
      }
    case AddConfiguration(name, graph) =>
      Builder.test(graph) match {
        case Result.Err(e) => sender() ! GraphFaulty(graph, name, e)
        case Result.Res(_) =>
          val str = Json.asciiStringify(Json.toJson(graph))
          val config = Tables.Configurations(name, str)
          runZIO{ load(query.insertOrUpdate(config).transactionally) }
      }
    case ChangeConfigName(oldName, newName) =>
      runZIO {
        load(getConfig(oldName).map(_.name).update(newName).transactionally)
      }
    case DeleteConfiguration(name) =>
      runZIO{ load(getConfig(name).delete) }
    case ClearAll => runZIO{ load(query.delete.transactionally) }
    case msg: AssignConfig => deviceActor forward msg
  }

  def runZIO[T](zio: Task[T]): T = zioRuntime.unsafeRun(zio)

  log.debug("Started")
}

object ConfigurationsActor {
  def props(broadcast: ActorRef,
            driver: ActorRef,
            socket: SocketActors)
           (implicit db: Database,
            locator: PluginLocator,
            profile: JdbcProfile): Props =
    Props(new ConfigurationsActor(broadcast, driver, socket))

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
  implicit val omap: MapGroup[Configuration.Graph] = _ ++ _

  case class RestoreConfigurations(configs: SMap[Result[Configuration.Graph]])

  case object GetConfigurations extends JsContract with ConfigurationFlow

  implicit val getConfigFormat: OFormat[GetConfigurations.type] = Json.format
  JsContract.add[GetConfigurations.type]("configurations-get")

  case class Configurations(configurations: SMap[Configuration.Graph]) extends JsContract

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

  case object GetConfigurationNodes extends JsContract with ConfigurationFlow

  implicit val getConfigNodesFormat: OFormat[GetConfigurationNodes.type] = Json.format
  JsContract.add[GetConfigurationNodes.type]("configuration-nodes-get")

  case class ConfigurationNodes(nodes: List[BlockDescription[_]]) extends JsContract with ConfigurationFlow

  implicit val XLetFormat: Format[XLet] = Json.format
  implicit val blockDescrFormat: OFormat[BlockDescription[_]] = new OFormat[BlockDescription[_]] {
    override def reads(json: JsValue): JsResult[BlockDescription[_]] = JsError()

    override def writes(o: BlockDescription[_]): JsObject = Json.obj(
      "name" -> o.name,
      "inputs" -> o.inputs,
      "outputs" -> o.outputs
    ) ++ blockTypeFormat.writes(o.blockType)
  }
  implicit val configNodesFormat: OFormat[ConfigurationNodes] = Json.format
  JsContract.add[ConfigurationNodes]("configuration-nodes")
}

