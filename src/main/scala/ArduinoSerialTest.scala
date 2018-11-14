import akka.NotUsed
import akka.actor.ActorSystem
import akka.event.Logging
import akka.stream.actor.ZeroRequestStrategy
import akka.stream.scaladsl.{Broadcast, Flow, Framing, GraphDSL, RunnableGraph, Sink, Source}
import akka.stream.{ActorMaterializer, Attributes, ClosedShape}
import akka.util.ByteString
import com.github.jarlakxen.reactive.serial.ReactiveSerial
import org.reactivestreams.Publisher

import scala.reflect.io.Directory

object ArduinoSerialTest extends App {

  implicit val actorSystem = ActorSystem("ReactiveSerial")
  implicit val materializer = ActorMaterializer()

  val ports = Directory("/dev/")
    .list
    .map(_.toString)
    .filter(_.startsWith("/dev/ttyACM"))
    .map(ReactiveSerial.port)
    .map(ReactiveSerial(_, 115200))
    .map(s => (s, s.publisher(bufferSize = 100)))
    .collect{ case (s: ReactiveSerial, p: Publisher[_]) => (p, s.subscriber(ZeroRequestStrategy)) }
    .map(x => (Source.fromPublisher(x._1), Sink.fromSubscriber(x._2)))
    .collect {
      case (src: Source[ByteString, _], sink: Sink[ByteString, _]) =>
        RunnableGraph.fromGraph(GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
          import GraphDSL.Implicits._

          val print = Flow[String].log("arduino-data")
          .withAttributes(
            Attributes.logLevels(
              onElement = Logging.WarningLevel,
              onFinish = Logging.InfoLevel,
              onFailure = Logging.DebugLevel
            )
          )

          val decode = Flow[ByteString].via(
            Framing.delimiter(ByteString("\r\n"), maximumFrameLength = 100, allowTruncation = true)
          ).map(_.utf8String)

          val reverse = Flow[String].map(_.reverse)
          val encode = Flow.fromFunction[String, ByteString](s => ByteString(s, "utf-8"))

          src ~> decode ~> print ~> reverse ~> encode ~> Sink.ignore

          ClosedShape
        })
    }
    .map(_.run)
    .toList

}
