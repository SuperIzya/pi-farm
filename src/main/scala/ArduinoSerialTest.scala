import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.actor.ZeroRequestStrategy
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, RunnableGraph, Sink, Source}
import akka.stream.{ActorMaterializer, ClosedShape}
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

          val bcast = builder.add(Broadcast[String](2))

          val prn = Flow[String].map(s => println(s"Arduino sais $s"))
          val decode = Flow.fromFunction[ByteString, String](_.decodeString("utf-8"))
          val reverse = Flow[String].map(_.reverse)
          val encode = Flow.fromFunction[String, ByteString](s => ByteString(s, "utf-8"))

          src ~> decode ~> bcast ~> reverse ~> encode ~> sink
                           bcast ~> prn ~> Sink.ignore
          ClosedShape
        })
    }
    .map(_.run)
    .toList

}
