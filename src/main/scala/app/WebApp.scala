package app

import loci.registry.{Binding, Registry}
import org.scalajs.dom
import org.scalajs.dom.{CanvasRenderingContext2D, document}
import org.scalajs.dom.html.{Canvas, Div, Image}
import rescala.default.*
import scalatags.JsDom.all.*
import app.Codecs.*
import kofre.base.Defs
import kofre.datatypes.{AddWinsSet}
import rescala.extra.distribution.Network
import kofre.dotted.Dotted

import loci.serializer.jsoniterScala.given
import rescala.extra.Tags.SignalTagListToScalatags

object WebApp {

  val peerId = Defs.genId()
  val registry = new Registry

  def main(args: Array[String]): Unit = {
    document.addEventListener("DOMContentLoaded", { (e: dom.Event) =>
      setupUI()
    })
  }

  def setupUI(): Unit = {

    //
    // MAP[remoteRef, peerId]
    //

    val remotesEvt = Evt[(String, String, Boolean)]()
    val remotes: Signal[Map[String, String]] = remotesEvt.fold(Map.empty[String, String]){ (current, info) =>
      // remove Map entry if false is passed, otherwise add to map
      if(!info._3){
        current - (info._1)
      } else {
        current + (info._1 -> info._2)
      }
    }

    remotes.changed.observe{ x =>
      println(s"====== REMOTES CHANGED ======")
      x.toList.foreach(x => println(x))
    }

    //
    // PEERS
    //

    val peersEvt = Evt[Peer]()
    val peers: Signal[Dotted[AddWinsSet[Peer]]] = peersEvt.fold(Dotted(AddWinsSet.empty[Peer])){ (current, peer) =>
      // disconnected peer-pairs should be removed by checking either the IDs match the disconnected ID, otherwise add new one
      if (peer.right == "disconnected") {
        val currentList: List[Peer] = current.named(peerId).elements.toList
        var temp: List[Peer] = List[Peer]()
        for (peerElem <- currentList) {
          if (peerElem.consistsString(peer.left)) {
            println(s"removing element $peerElem")
            temp = peerElem :: temp
          }
        }
        current.named(peerId).removeAll(temp).anon
      } else {
        current.named(peerId).add(peer).anon
      }
    }

    Network.replicate(peers, None, registry)(Binding("peers"))

    peers.changed.observe { x =>
      println(s"====== PEERS CHANGED ======")
      x.store.elements.toList.foreach(x => println(x))
    }

    //
    // STATUS
    //

    val statusesEvt = Evt[Status]()
    val statuses: Signal[Dotted[AddWinsSet[Status]]] = statusesEvt.fold(Dotted(AddWinsSet.empty[Status])) { (current, status) =>
      // remove entry from the set if the member remove is true, otherwise add to the set (won't be added if the pair is already there)
      if (status.remove) {
        val currentList: List[Status] = current.named(peerId).elements.toList
        var index: Option[Status] = None
        for (n <- 0 to currentList.length - 1) {
          if (currentList(n) == status) {
            index = Some(status)
          }
        }
        index match {
          case None => current
          case Some(value) => {
            current.named(peerId).remove(value).anon
          }
        }
      } else {
        current.named(peerId).add(status).anon
      }
    }

    Network.replicate(statuses, Some(statusesEvt), registry)(Binding("statuses"))

    statusesEvt.observe{ x =>
      println(s"====== PEER STATUSES CHANGED ======")
      println(x)
      val listStatus: List[Status] = statuses.now.store.elements.toList

      // do the following if the status is in the list and members "remove" and "status" are false
      if(listStatus.contains(x) && !x.remove && !x.status){
        // this will find and delete the peer-pairs that are disconnected
        peersEvt(new Peer(remotes.now.get(x.ref).get, "disconnected"))
        // this will remove the map of the remote reference
        remotesEvt.fire((x.ref, "", false))
        // this will remove the temporary entry that was just created
        statusesEvt.fire(new Status(x.id, x.status, true, x.ref))
      }
    }

    val e = statuses.changed
    e.observe{x =>
      val listStatus: List[Status] = x.store.elements.toList
      println(s"====== STATUSES CHANGED ======")
      listStatus.foreach(x => println(x))

      // do the following if length of list is 2 and member "remove" in both elements are false
      if (listStatus.length == 2 && !listStatus(0).remove && !listStatus(1).remove) {
        val peerOne = listStatus(0)
        val peerTwo = listStatus(1)
        val peerToAdd = if (peerId != peerOne.id) peerOne.id else peerTwo.id
        val refToAdd = if (peerId == peerOne.id) peerOne.ref else peerTwo.ref

        // add new remote reference in Map
        if (!remotes.now.contains(refToAdd)){
          remotesEvt((refToAdd, peerToAdd, true))
        }

        // create the peer-pair
        peersEvt.fire(new Peer(peerOne.id, peerTwo.id))

        // delete the elements in the list
        statusesEvt.fire(new Status(peerOne.id, true, true, peerOne.ref))
        statusesEvt.fire(new Status(peerTwo.id, true, true, peerTwo.ref))
      }
    }


    val contentPeers: Set[Peer] = peers.now.store.elements
    contentPeers.foreach(x => println(x))

    val contentPeerStatus: Set[Status] = statuses.now.store.elements
    contentPeerStatus.foreach(x => println(x))


    //
    // UI
    //

    val canvasElem = canvas(
      border := "1px solid red",
      width := "100%",
      height := "100%",
    ).render

    val divWebRTC = div(
      `class` := "row",
      WebRTCHandling(registry).webrtcHandlingArea.render
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
//      div(display.asModifierL),
      divCanvas
    ).render

    document.body.appendChild(gridElem)
    drawNetwork(canvasElem, divCanvas)

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
