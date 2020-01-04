package com.ilyak.pifarm.types

import akka.stream.UniformFanOutShape
import cats.Monoid
import cats.implicits._
import com.ilyak.pifarm.flow.configuration.Connection.Sockets

import scala.language.implicitConversions

case class GraphState(kill: UniformFanOutShape[Int, Int],
                      map: SMap[Sockets],
                      creators: SMap[GState[Sockets]])

object GraphState {
  def apply(kill: UniformFanOutShape[Int, Int]): GraphState =
    new GraphState(kill, Map.empty, Map.empty)

  def runCreator(create: GState[Sockets], name: String): GState[Sockets] =
    for {
      sockets <- create
      _ <- GraphState.add(name, sockets)
    } yield sockets

  def retrieveSockets(name: String): GState[Sockets] =
    GState { state =>
      state.map
        .get(name)
        .map((state, _))
        .map(GBuilder.pure)
        .getOrElse {
          state.creators
            .get(name)
            .map(runCreator(_, name).run(state))
            .getOrElse(GBuilder.pure(state -> Sockets.empty))
        }
    }

  def add(name: String, creator: GState[Sockets]): GState[Unit] =
    for { _ <- runCreator(creator, name) } yield ()

  def add(name: String, sockets: Sockets): GState[Sockets] = GState { state =>
    GBuilder.pure(state.add(name, sockets) -> sockets)
  }

  implicit final class StateOpsC(val lhs: GraphState) extends AnyVal {
    private def addM[T: Monoid](key: String,
                                value: T,
                                map: SMap[T],
                                copy: SMap[T] => GraphState): GraphState = {
      copy(map |+| Map(key -> value))
    }

    private def copySockets(s: SMap[Sockets]): GraphState = lhs.copy(map = s)
    private def copyCreators(s: SMap[GState[Sockets]]): GraphState =
      lhs.copy(creators = s)

    def add(key: String, v: Sockets): GraphState =
      addM(key, v, lhs.map, copySockets)

    def add(key: String, v: GState[Sockets]): GraphState =
      addM(key, v, lhs.creators, copyCreators)
  }
}
