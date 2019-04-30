package com.ilyak.pifarm.flow.actors

import akka.actor.{ Actor, Props }
import com.ilyak.pifarm.Result
import com.ilyak.pifarm.Types.SMap
import com.ilyak.pifarm.control.configuration.Builder
import com.ilyak.pifarm.flow.actors.ConfigurationActor.{ AddConfiguration, Configurations, GetConfigurations, GraphFaulty }
import com.ilyak.pifarm.flow.configuration.Configuration
import com.ilyak.pifarm.plugins.PluginLocator
import slick.jdbc.JdbcBackend.Database

class ConfigurationActor(implicit db: Database, locator: PluginLocator) extends Actor {

  var configurations: SMap[Configuration.Graph] = Map.empty

  override def receive: Receive = {
    case GetConfigurations => sender() ! Configurations(configurations)
    case AddConfiguration(name, graph) =>
      Builder.test(graph) match {
        case Result.Err(e) => sender() ! GraphFaulty(graph, name, e)
        case Result.Res(_) =>

      }
  }
}

object ConfigurationActor {
  def props()(implicit db: Database, locator: PluginLocator): Props =
    Props(new ConfigurationActor())

  case object GetConfigurations
  case class Configurations(list: SMap[Configuration.Graph])
  case class AddConfiguration(name: String, graph: Configuration.Graph)
  case class GraphFaulty(graph: Configuration.Graph, name: String, errors: String)
}