package app

import loci.registry.{Binding, Registry}
import org.scalajs.dom
import org.scalajs.dom.{CanvasRenderingContext2D, document}
import org.scalajs.dom.html.{Canvas, Div, Image}
import rescala.default.*
import scalatags.JsDom.all.*
import app.Codecs.*
import kofre.datatypes.{AddWinsSet, RGA}
import rescala.extra.distribution.Network
import kofre.decompose.containers.DeltaBufferRDT
import kofre.syntax.DottedName

import java.util.concurrent.ThreadLocalRandom
import scala.util.Random

import loci.serializer.Serializable.resolutionFailure

case class Peer(left: String, right: String)

object TutorialApp {

  val peerId: String = ThreadLocalRandom.current().nextLong().toHexString
  val registry = new Registry

  def main(args: Array[String]): Unit = {
    document.addEventListener("DOMContentLoaded", { (e: dom.Event) =>
      setupUI()
    })
  }

  def setupUI(): Unit = {

    val canvasElem = canvas(
      border := "1px solid red",
      width := "100%",
      height := "100%",
    ).render

    val divWebRTC = div(
      `class` := "row",
      WebRTCHandling(registry, peerId).webrtcHandlingArea.render
    ).render

    val divCanvas = div(
      `class` := "row",
      div(
        `class` := "col",
        canvasElem,
      )
    ).render

    val gridElem = div(
      `class` := "container",
      divWebRTC,
      divCanvas
    ).render

    val connectedPeers = Evt[Peer]()

    val peers: Signal[DeltaBufferRDT[AddWinsSet[Peer]]] = Storing.storedAs("peers", DeltaBufferRDT( peerId, AddWinsSet.empty[Peer])){ init =>
      connectedPeers.fold(init) { (current, peer) =>
        // needs container, that's why using DeltaBufferRDT
        current.add(peer)
      }
    }(peerAddWinsSetCodec)

    // No given instance of type kofre.base.Lattice[kofre.datatypes.AddWinsSet[app.Peer]] was found for an implicit parameter of method replicate in object Network
    Network.replicate(peers, registry)(Binding("peers"))

    document.body.appendChild(gridElem)
    drawNetwork(canvasElem, divCanvas)

    connectedPeers.fire(new Peer("testLeft2", "testRight3"))
    val contentPeers: Set[Peer] = peers.now.state.store.elements
    contentPeers.foreach(x => println(x))

  }

  def drawNetwork(canvasElem: Canvas, divCanvas: Div): Unit = {

    val ctx = canvasElem.getContext("2d").asInstanceOf[CanvasRenderingContext2D]

    // make the canvas not pixelated
    canvasElem.width = divCanvas.getBoundingClientRect().width.toInt
    //    canvasElem.height = divCanvas.getBoundingClientRect().height.toInt
    canvasElem.height = dom.window.innerHeight.toInt

    ctx.lineWidth = 3
    ctx.strokeStyle = "green"

    ctx.beginPath()
    ctx.moveTo(100 / 2, 100 / 2)
    ctx.lineTo(600 + 100 / 2, 25 + 100 / 2)

    ctx.moveTo(100 / 2, 100 / 2)
    ctx.lineTo(300 + 100 / 2, 105 + 100 / 2)

    ctx.moveTo(600 + 100 / 2, 25 + 100 / 2)
    ctx.lineTo(300 + 100 / 2, 105 + 100 / 2)

    ctx.stroke()

    // create the canvas
    val image = document.createElement("img").asInstanceOf[Image]
    image.src = "images/circle.png"
    image.onload = (e: dom.Event) => {
      ctx.drawImage(image, 0, 0, 100, 100)
      ctx.drawImage(image, 600, 25, 100, 100)
      ctx.drawImage(image, 300, 105, 100, 100)
    }

  }

}
