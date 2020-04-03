package zio.keeper.membership

import zio.ZIO
import zio.keeper.membership.PingPong.{Ping, Pong}
import zio.keeper.membership.swim.{Message, Protocol}
import zio.stream.ZStream
import zio.test._
import zio.test.Assertion._

object ProtocolSpec extends DefaultRunnableSpec  {

  val protocolDefinition = Protocol[PingPong](
    {
      case Message.Direct(sender, Ping(i)) =>
        ZIO.succeed(Option(Message.Direct(sender, Pong(i))))
      case _ => ZIO.none
    },
    ZStream.empty
  )

  val testNode = NodeAddress(Array(1,2,3,4), 123)

  val spec = suite("protocol spec")(
    testM("request response"){
      for {
        protocol <- protocolDefinition
        response <- protocol.onMessage(Message.Direct(testNode, Ping(123)))
      } yield assert(response)(isSome(equalTo(Message.Direct(testNode, Pong(123)))))
    },
    testM("binary request response"){
      for {
        protocol <- protocolDefinition.map(_.binary)
        binaryMessage <- TaggedCodec.write[PingPong](Ping(123))
        responseBinary <- protocol.onMessage(Message.Direct(testNode, binaryMessage))
        response <- responseBinary match {
          case Some(Message.Direct(addr, chunk)) => TaggedCodec.read[PingPong](chunk).map(pp => Some(Message.Direct(addr, pp)))
          case _ => ZIO.none
        }
      } yield assert(response)(isSome(equalTo(Message.Direct[PingPong](testNode, Pong(123)))))
    }
  )

}
