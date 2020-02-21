package zio.keeper.membership.swim.protocols

import upickle.default.{ readBinary, writeBinary }
import zio.keeper.SerializationError._
import zio.keeper.membership.NodeAddress
import zio.keeper.membership.swim.Protocol
import zio.keeper.{ ByteCodec, TaggedCodec }
import zio.stream.ZStream
import zio.{ Chunk, IO, ZIO }

case class User[A](msg: A)

object User {

  implicit def taggedRequests[A, B](
    implicit
    u: ByteCodec[User[A]]
  ): TaggedCodec[User[A]] =
    TaggedCodec.instance(
      { _: User[_] =>
        101
      }, {
        case 101 => u.asInstanceOf[ByteCodec[User[A]]]
      }
    )

  implicit def codec[A: TaggedCodec]: ByteCodec[User[A]] =
    new ByteCodec[User[A]] {

      override def fromChunk(chunk: Chunk[Byte]): IO[DeserializationTypeError, User[A]] =
        ZIO
          .effect(readBinary[Array[Byte]](chunk.toArray))
          .mapError(DeserializationTypeError(_))
          .flatMap { b1 =>
            TaggedCodec.read[A](Chunk.fromArray(b1))
          }
          .map(ab => User(ab))

      override def toChunk(a: User[A]): IO[SerializationTypeError, Chunk[Byte]] =
        TaggedCodec
          .write[A](a.msg)
          .flatMap { ch1 =>
            ZIO
              .effect(
                Chunk.fromArray(
                  writeBinary[Array[Byte]](ch1.toArray)
                )
              )
              .mapError(SerializationTypeError(_))
          }
    }

  def protocol[B: TaggedCodec](
    userIn: zio.Queue[(NodeAddress, B)],
    userOut: zio.Queue[(NodeAddress, B)]
  ) =
    Protocol[NodeAddress, User[B]].apply(
      (s, u: User[B]) => userIn.offer((s, u.msg)).as(None),
      ZStream
        .fromQueue(userOut)
        .map {
          case (recipient, msg) => (recipient, User(msg))
        }
    )

}
