package app

import com.github.plokhotnyuk.jsoniter_scala.core.{JsonReader, JsonValueCodec, JsonWriter}
import com.github.plokhotnyuk.jsoniter_scala.macros.{CodecMakerConfig, JsonCodecMaker}
import kofre.datatypes.RGA
import loci.transmitter.IdenticallyTransmittable


object Codecs {

  implicit val peerSetCodec: JsonValueCodec[Set[Peer]] = JsonCodecMaker.make
//  implicit val peerTransmittable: IdenticallyTransmittable[Peer] = IdenticallyTransmittable()
//  implicit val peerSetTransmittable: IdenticallyTransmittable[Set[Peer]] = IdenticallyTransmittable()

}
