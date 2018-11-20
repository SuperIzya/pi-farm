
import java.io.{File, FilenameFilter}

import akka.NotUsed
import akka.actor.ActorSystem
import akka.event.Logging
import akka.stream.actor.ZeroRequestStrategy
import akka.stream.scaladsl._
import akka.stream.{ActorMaterializer, Attributes, ClosedShape}
import akka.util.ByteString
import com.fazecast.jSerialComm.SerialPort
import com.github.jarlakxen.reactive.serial.{Port, ReactiveSerial}
import org.reactivestreams.{Publisher, Subscriber}

import scala.concurrent.duration._
import scala.language.postfixOps

object ArduinoSerialTest extends App {
  val encode: String => ByteString = ByteString(_, "utf-8")
  val separator = "\n"
  val sourceDelimiter = encode(";")

  implicit val actorSystem = ActorSystem("RaspberryFarm")
  implicit val materializer = ActorMaterializer()

  val isArduinoLog: String => Boolean = _.trim.startsWith("log:")

  val baudRate = 9600

  val ports = new File("/dev")
    .listFiles(new FilenameFilter {
      override def accept(file: File, s: String): Boolean = s.startsWith("ttyACM")
    })
    .toList
    .map(f => SerialPort.getCommPort(f.getAbsolutePath))
    .map(new Port(_))
    .map(ReactiveSerial(_, baudRate))
    .map(s => (s.publisher(bufferSize = 100), s.subscriber(ZeroRequestStrategy)))
    .collect {
      case (p: Publisher[ByteString], s: Subscriber[ByteString]) =>
        val source = Source.fromPublisher(p)
        val sink = Sink.fromSubscriber(s)

        RunnableGraph.fromGraph(GraphDSL.create() { implicit builder =>
          import GraphDSL.Implicits._

          val bcast = builder.add(Broadcast[String](2))

          source ~> decode ~> bcast

          bcast ~> process ~> logArduinoData ~> finishMessage ~> sink
          bcast ~> logArduinoDebug ~> Sink.ignore

          ClosedShape
        })
    }
    .map(_.run)

  def finishMessage(implicit builder: GraphDSL.Builder[NotUsed]) =
    Flow[String]
      .filter(!_.isEmpty)
      .map(_ + separator)
      .map(encode)


  def process(implicit builder: GraphDSL.Builder[NotUsed]) =
    Flow[String].filter(!isArduinoLog(_)).map(_.toUpperCase)

  def decode(implicit builder: GraphDSL.Builder[NotUsed]) =
    Flow[ByteString].via(
      Framing.delimiter(sourceDelimiter, maximumFrameLength = 200, allowTruncation = true)
    ).map(_.utf8String)
      .throttle(1, 1 second)

  def logArduinoData(implicit builder: GraphDSL.Builder[NotUsed]) =
    Flow[String]
      .log("arduino-data")
      .withAttributes(
        Attributes.logLevels(
          onElement = Logging.WarningLevel,
          onFinish = Logging.InfoLevel,
          onFailure = Logging.DebugLevel
        )
      )

  def logArduinoDebug(implicit builder: GraphDSL.Builder[NotUsed]) =
    Flow[String].filter(isArduinoLog)
      .log("arduino-log")
      .withAttributes(
        Attributes.logLevels(
          onElement = Logging.InfoLevel,
          onFinish = Logging.InfoLevel,
          onFailure = Logging.DebugLevel
        )
      )
}
