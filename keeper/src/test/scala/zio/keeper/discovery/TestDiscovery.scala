package zio.keeper.discovery

import zio._
import zio.keeper.{ NodeAddress, TransportError }
import zio.logging.Logging
import zio.logging._
import zio.nio.core.InetSocketAddress
import zio.random._

object TestDiscovery {

  type TestDiscovery = Has[Service]

  def addMember(m: NodeAddress): URIO[TestDiscovery, Unit] =
    URIO.accessM[TestDiscovery](_.get.addMember(m))

  def removeMember(m: NodeAddress): URIO[TestDiscovery, Unit] =
    URIO.accessM[TestDiscovery](_.get.removeMember(m))

  val nextPort: URIO[TestDiscovery, Int] =
    URIO.accessM[TestDiscovery](_.get.nextPortNumber)

  val live: ZLayer[Logging with Random, Nothing, Discovery with TestDiscovery] =
    ZLayer.fromEffectMany {
      for {
        logger     <- ZIO.environment[Logging]
        _          <- logger.get.info("creating test discovery")
        nodes      <- Ref.make(Set.empty[NodeAddress])
        randomPort <- nextIntBounded(20000).map(_ + 10000)
        ports      <- Ref.make(randomPort)
        test       = new Test(nodes, ports, logger.get)
      } yield Has.allOf[Discovery.Service, Service](test, test)
    }

  trait Service extends Discovery.Service {
    def addMember(m: NodeAddress): UIO[Unit]
    def removeMember(m: NodeAddress): UIO[Unit]
    def nextPortNumber: UIO[Int]
  }

  private class Test(ref: Ref[Set[NodeAddress]], port: Ref[Int], logger: Logger[String]) extends Service {

    val discoverNodes: IO[TransportError, Set[InetSocketAddress]] =
      for {
        members <- ref.get
        addrs   <- IO.collectAll(members.map(_.socketAddress))
      } yield addrs.toSet

    def addMember(m: NodeAddress): UIO[Unit] =
      logger.info("adding node: " + m) *>
        ref.update(_ + m).unit

    def removeMember(m: NodeAddress): UIO[Unit] =
      logger.info("removing node: " + m) *>
        ref.update(_ - m).unit

    def nextPortNumber: UIO[Int] =
      port.updateAndGet(_ + 1)
  }
}
