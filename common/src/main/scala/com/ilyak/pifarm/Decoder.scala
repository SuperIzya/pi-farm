package com.ilyak.pifarm

import akka.stream.stage._
import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import com.ilyak.pifarm.Decoder.DecoderF
import com.ilyak.pifarm.Decoder.Trie.PrefixForest
import com.ilyak.pifarm.types.Result

import scala.language.implicitConversions

case class Decoder[T](decode: DecoderF[T], prefix: String)

object Decoder {
  type DecoderF[T] = String => List[T]

  def apply[T: Decoder]: Decoder[T] = implicitly[Decoder[T]]

  implicit def toDecoder[T](p: (String, DecoderF[T])): Decoder[T] =
    new Decoder(p._2, p._1)

  def merge(decoders: Iterable[Decoder[_]],
            ignoreDup: Boolean = false): Result[PrefixForest] =
    decoders.foldLeft[Result[PrefixForest]](Result.Res(emptyForest)) { (a, b) =>
      a.flatMap(_.add(b, ignoreDup))
    }

  case class Trie(key: String,
                  value: Option[Decoder[_]],
                  children: PrefixForest)

  private def emptyForest: PrefixForest = Map.empty

  object Trie {
    type PrefixForest = Map[Char, Trie]
    private val alphabet: String =
      "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 -_"

    private def getIndex(str: String): Result[Int] = {
      val index = alphabet.indexOf(str(0))
      if (index < 0) Result.Err(s"Symbol ${str(0)} not allowed")
      else Result.Res(index)
    }

    implicit class TrieOps(val forest: PrefixForest) extends AnyVal {
      def getPrefix(key: String): Result[Decoder[_]] =
        getIndex(key)
          .flatMap { _ =>
            forest
              .get(key(0))
              .map(Result.Res(_))
              .getOrElse(Result.Err(s"No decoder for key $key"))
          }
          .flatMap { t =>
            if (t.key == key)
              t.value
                .map(Result.Res(_))
                .getOrElse(Result.Err(s"Empty decoder for key '$key'"))
            else if (!key.startsWith(t.key)) Result.Err(s"Unknown key '$key'")
            else t.children.getPrefix(key.substring(t.key.length))
          }

      def add(decoder: Decoder[_], ignoreDup: Boolean): Result[PrefixForest] =
        add(decoder.prefix, decoder, ignoreDup)

      def add(key: String,
              decoder: Decoder[_],
              ignoreDup: Boolean): Result[PrefixForest] = {
        getIndex(key) match {
          case Result.Err(e) => Result.Err(s"$e in key '$key'")
          case Result.Res(_) =>
            forest.get(key(0)) match {
              case None =>
                Result.Res(
                  forest ++ Map(key(0) -> Trie(key, Some(decoder), emptyForest))
                )
              case Some(trie) if key == trie.key && trie.value.isEmpty =>
                Result.Res(
                  forest ++ Map(key(0) -> trie.copy(value = Some(decoder)))
                )
              case Some(trie) if key == trie.key && trie.value.isDefined =>
                if (ignoreDup) Result.Res(forest)
                else Result.Err(s"Decoder already defined for key $key")
              case Some(trie) if key.startsWith(trie.key) =>
                trie.children
                  .add(key.substring(0, trie.key.length), decoder, ignoreDup)
                  .map(f => forest ++ Map(key(0) -> trie.copy(children = f)))
              case Some(trie) =>
                val sharedLength: Int = key
                  .zip(trie.key)
                  .collect {
                    case (a, b) if a == b => true
                    case _                => false
                  }
                  .takeWhile(x => x)
                  .size

                val sharedKey = key.substring(0, sharedLength)

                val newKey = key.substring(sharedLength)
                val oldKey = trie.key.substring(sharedLength)

                Result.Res {
                  forest ++ Map(
                    sharedKey(0) -> Trie(
                      sharedKey,
                      None,
                      Map(
                        newKey(0) -> Trie(newKey, Some(decoder), Map.empty),
                        oldKey(0) -> trie.copy(key = oldKey)
                      )
                    )
                  )
                }
            }
        }
      }
    }

  }

  class DecoderShape(forest: PrefixForest)
      extends GraphStage[FlowShape[String, List[Any]]] {
    val in: Inlet[String] = Inlet("Decoding shape inlet")
    val out: Outlet[List[Any]] = Outlet("Decoding shape outlet")

    override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
      new GraphStageLogic(shape) with StageLogging {
        var last: Option[List[Any]] = None

        def push(lst: List[Any]): Boolean =
          if (isAvailable(out)) {
            push(out, lst)
            true
          } else false

        setHandler(
          in,
          new InHandler {
            override def onPush(): Unit = {
              val msg = grab(in)
              val split = msg.split(':')
              forest.getPrefix(split(0)) match {
                case Result.Res(decoder) =>
                  val l = decoder.decode(split(1))
                  if (!push(l)) last = Some(l)
                case Result.Err(e) =>
                  log.error(s"Failed to decode message '$msg' due to '$e'")
              }
              pull(in)
            }
          }
        )

        setHandler(out, new OutHandler {
          override def onPull(): Unit = {
            last.foreach(push(out, _))
            last = None
          }
        })

        override def preStart(): Unit = pull(in)
      }

    override def shape: FlowShape[String, List[Any]] = FlowShape(in, out)
  }

}
