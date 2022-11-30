package app

import loci.registry.Registry
import org.scalajs.dom
import org.scalajs.dom.{CanvasRenderingContext2D, document}
import org.scalajs.dom.html.{Canvas, Div, Image}
import rescala.default._
import rescala.extra.lattices.sequences.RGA
import rescala.extra.lattices.sequences.RGA.RGA
import scalatags.JsDom.all._
import app.Codecs._

import scala.util.Random

case class Peer(left: String, right: String)

object TutorialApp {

  val registry = new Registry

  def main(args: Array[String]): Unit = {
    document.addEventListener("DOMContentLoaded", { (e: dom.Event) =>
      setupUI()
    })
  }

  def setupUI(): Unit = {

    val id = new Random().nextInt()
    println(id)
//    dom.window.localStorage.setItem("id", id.toString)

    val canvasElem = canvas(
      border := "1px solid red",
      width := "100%",
      height := "100%",
    ).render

    val divWebRTC = div(
      `class` := "row",
      WebRTCHandling(registry, id).webrtcHandlingArea.render
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

    val connectedPeers = Evt[Peer]

    val peers: Signal[RGA[Peer]] = Storing.storedAs("peers", RGA.empty[Peer]){ init =>
      connectedPeers.fold(init) { (current, peer) =>
        current.prepend(peer)
      }
    }

    document.body.appendChild(gridElem)
    drawNetwork(canvasElem, divCanvas)
    println(peers)

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