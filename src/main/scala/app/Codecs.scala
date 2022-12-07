package app

import app.WebApp.peerId
import com.github.plokhotnyuk.jsoniter_scala.core.{JsonReader, JsonValueCodec, JsonWriter}
import com.github.plokhotnyuk.jsoniter_scala.macros.{CodecMakerConfig, JsonCodecMaker}
import kofre.datatypes.{AddWinsSet}
import loci.transmitter.IdenticallyTransmittable
import kofre.dotted.{Dotted}

object Codecs {

  implicit val peerSetTransmittable: IdenticallyTransmittable[Dotted[AddWinsSet[Peer]]] = IdenticallyTransmittable()
  implicit val codecState: JsonValueCodec[Dotted[AddWinsSet[Peer]]] = JsonCodecMaker.make(CodecMakerConfig.withMapAsArray(true))

  implicit val statusSetTransmittable: IdenticallyTransmittable[Dotted[AddWinsSet[Status]]] = IdenticallyTransmittable()
  implicit val statusCodecState: JsonValueCodec[Dotted[AddWinsSet[Status]]] = JsonCodecMaker.make(CodecMakerConfig.withMapAsArray(true))

  // MIGHT be useful later :)
//  implicit val peerSetCodec: JsonValueCodec[Set[Peer]] = JsonCodecMaker.make
//  implicit val peerTransmittable: IdenticallyTransmittable[Peer] = IdenticallyTransmittable()
//  implicit val peerSetTransmittable: IdenticallyTransmittable[Set[Peer]] = IdenticallyTransmittable()

//  implicit val codecState: JsonValueCodec[Dotted[AddWinsSet[Peer]]] = JsonCodecMaker.make(CodecMakerConfig.withMapAsArray(true))
//
//  implicit val peerAddWinsSetCodec: JsonValueCodec[DottedName[AddWinsSet[Peer]]] =
//    new JsonValueCodec[DottedName[AddWinsSet[Peer]]]:
//      override def decodeValue(in: JsonReader, default: DottedName[AddWinsSet[Peer]]): DottedName[AddWinsSet[Peer]] = {
//        val state = codecState.decodeValue(in, default.anon)
//        new DottedName(peerId, state)
//      }
//
//      override def encodeValue(x: DottedName[AddWinsSet[Peer]], out: JsonWriter): Unit = codecState.encodeValue(x.anon, out)
//
//      override def nullValue: DottedName[AddWinsSet[Peer]] = DottedName(peerId, Dotted(AddWinsSet.empty[Peer]))


//  implicit val peerStatusListTransmittable: IdenticallyTransmittable[Dotted[RGA[StatusClass]]] = IdenticallyTransmittable()
//  @nowarn()
//  implicit val peerStatusCodecState: JsonValueCodec[Dotted[RGA[StatusClass]]] = JsonCodecMaker.make(CodecMakerConfig.withMapAsArray(true))
}
