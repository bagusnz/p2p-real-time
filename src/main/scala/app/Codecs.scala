package app

import com.github.plokhotnyuk.jsoniter_scala.core.{JsonReader, JsonValueCodec, JsonWriter}
import com.github.plokhotnyuk.jsoniter_scala.macros.{CodecMakerConfig, JsonCodecMaker}
import kofre.datatypes.RGA


object Codecs {

  implicit val peersetCodec: JsonValueCodec[Set[Peer]] = JsonCodecMaker.make

}
