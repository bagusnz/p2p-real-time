package app

import app.TutorialApp.peerId
import com.github.plokhotnyuk.jsoniter_scala.core.{JsonReader, JsonValueCodec, JsonWriter}
import com.github.plokhotnyuk.jsoniter_scala.macros.{CodecMakerConfig, JsonCodecMaker}
import kofre.datatypes.{AddWinsSet, RGA}
import loci.transmitter.IdenticallyTransmittable
import kofre.decompose.containers.DeltaBufferRDT
import kofre.dotted.{DotSet, Dotted}
import sourcecode.Text.generate


object Codecs {

//  implicit val peerSetCodec: JsonValueCodec[Set[Peer]] = JsonCodecMaker.make
//  implicit val peerTransmittable: IdenticallyTransmittable[Peer] = IdenticallyTransmittable()
//  implicit val peerSetTransmittable: IdenticallyTransmittable[Set[Peer]] = IdenticallyTransmittable()

  implicit val codecState: JsonValueCodec[Dotted[AddWinsSet[Peer]]] = JsonCodecMaker.make(CodecMakerConfig.withMapAsArray(true))
  implicit val peerAddWinsSetCodec: JsonValueCodec[DeltaBufferRDT[AddWinsSet[Peer]]] =
    new JsonValueCodec[DeltaBufferRDT[AddWinsSet[Peer]]]:
      override def decodeValue(in: JsonReader, default: DeltaBufferRDT[AddWinsSet[Peer]]): DeltaBufferRDT[AddWinsSet[Peer]] = {
        val state = codecState.decodeValue(in, default.state)
        new DeltaBufferRDT[AddWinsSet[Peer]](state, peerId, List())
      }

      override def encodeValue(x: DeltaBufferRDT[AddWinsSet[Peer]], out: JsonWriter): Unit = codecState.encodeValue(x.state, out)

      override def nullValue: DeltaBufferRDT[AddWinsSet[Peer]] = DeltaBufferRDT(peerId, AddWinsSet.empty[Peer])

}
