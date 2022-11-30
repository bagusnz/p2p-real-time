package app

import com.github.plokhotnyuk.jsoniter_scala.core.{JsonReader, JsonValueCodec, JsonWriter}
import com.github.plokhotnyuk.jsoniter_scala.macros.{CodecMakerConfig, JsonCodecMaker}
import rescala.extra.lattices.sequences.RGA.RGA
import rescala.extra.lattices.sequences.{LatticeSequence, RGA, Vertex}
import rescala.extra.lattices.sets.TwoPSet


object Codecs {
//  implicit val stringCodec: JsonValueCodec[String] = JsonCodecMaker.make[String]
//  implicit val listCodec: JsonValueCodec[List[(Int, Int)]] = JsonCodecMaker.make[List[(Int, Int)]]

//  implicit val rgaTransmittable = IdenticallyTransmittable[RGA[(Int, Int)]]()

  type Rgatuple = (TwoPSet[Vertex], Map[Vertex, Vertex], Map[Vertex, Peer])

  def rgaToTuple(rga: RGA[Peer]): Rgatuple = rga match {
    case LatticeSequence(vertices: TwoPSet[Vertex], edges: Map[Vertex, Vertex], values: Map[Vertex, Peer]) =>
      (vertices, edges, values)
  }

  val vcodec: JsonValueCodec[(TwoPSet[Vertex], Map[Vertex, Vertex], Map[Vertex, Peer])] =
    JsonCodecMaker.make(CodecMakerConfig.withMapAsArray(true))

  implicit val rgaCodec: JsonValueCodec[RGA[Peer]] = new JsonValueCodec[RGA[Peer]] {
    override def decodeValue(in: JsonReader, default: RGA[Peer]): RGA[Peer] = {
      val (vertices, edges, values) = vcodec.decodeValue(in, rgaToTuple(default))
      LatticeSequence(vertices, edges, values)
    }

    override def encodeValue(x: RGA[Peer], out: JsonWriter): Unit =
      vcodec.encodeValue(rgaToTuple(x), out)

    override def nullValue: RGA[Peer] = RGA.empty[Peer]
  }
}
