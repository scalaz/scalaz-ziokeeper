package zio.keeper
import zio.{ Chunk, ZIO }
import zio.stream.ZStream

package object membership extends Membership.Service[Membership] {


  override def broadcast(data: Chunk[Byte]): ZIO[Membership, Error, Unit] =
    ZIO.accessM[Membership](_.membership.broadcast(data))

  //conv
  override val events: ZStream[Membership, Error, MembershipEvent] =
    ZStream.unwrap(ZIO.access[Membership](_.membership.events))

  //conv
  override val localMember: ZIO[Membership, Nothing, Member] =
    ZIO.accessM[Membership](_.membership.localMember)

  //conv
  override val nodes: ZIO[Membership, Nothing, List[NodeId]] =
    ZIO.accessM[Membership](_.membership.nodes)

  //conv
  override val receive: ZStream[Membership, Error, Message] =
    ZStream.unwrap(ZIO.access[Membership](_.membership.receive))

  override def send(data: Chunk[Byte], receipt: NodeId): ZIO[Membership, Error, Unit] =
    ZIO.accessM[Membership](_.membership.send(data, receipt))
}