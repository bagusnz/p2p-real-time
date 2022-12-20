package app

import loci.registry.{Binding, Registry}
import org.scalajs.dom
import org.scalajs.dom.{CanvasRenderingContext2D, document}
import org.scalajs.dom.html.{Canvas, Div, Image}
import rescala.default.*
import scalatags.JsDom.all.*
import app.Codecs.*
import kofre.base.Defs
import kofre.datatypes.AddWinsSet
import rescala.extra.distribution.Network
import kofre.dotted.Dotted
import loci.serializer.jsoniterScala.given
import rescala.extra.Tags.SignalTagListToScalatags

import scala.math.*

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
      p(s"Peer ID = $peerId"),
      divCanvas
    ).render

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
//      println(s"====== REMOTES CHANGED ======")
//      x.toList.foreach(x => println(x))
    }

    //
    // PEER PAIRS
    //

    val peerPairsEvt = Evt[PeerPair]()
    val peerPairs: Signal[Dotted[AddWinsSet[PeerPair]]] = peerPairsEvt.fold(Dotted(AddWinsSet.empty[PeerPair])){ (current, peerPair) =>
      // disconnected peer-pairs should be removed by checking either the IDs match the disconnected ID, otherwise add new one
      if (peerPair.right == "disconnected") {
        val currentList: List[PeerPair] = current.named(peerId).elements.toList
        var temp: List[PeerPair] = List[PeerPair]()
        for (peerPairElem <- currentList) {
          if (peerPairElem.consistsString(peerPair.left)) {
            println(s"removing element $peerPairElem")
            temp = peerPairElem :: temp
          }
        }
        current.named(peerId).removeAll(temp).anon
      } else {
        current.named(peerId).add(peerPair).anon
      }
    }

    Network.replicate(peerPairs, None, registry)(Binding("peers"))

    peerPairs.changed.observe { x =>
//      println(s"====== PEERS CHANGED ======")
//      x.store.elements.toList.foreach(x => println(x))
      drawNetwork(peerPairs, canvasElem, divCanvas)
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
//      println(s"====== PEER STATUSES CHANGED ======")
//      println(x)
      val listStatus: List[Status] = statuses.now.store.elements.toList

      // do the following if the status is in the list and members "remove" and "status" are false
      if(listStatus.contains(x) && !x.remove && !x.status){
        // this will find and delete the peer-pairs that are disconnected
        peerPairsEvt(new PeerPair(remotes.now.get(x.ref).get, "disconnected"))
        // this will remove the map of the remote reference
        remotesEvt.fire((x.ref, "", false))
        // this will remove the temporary entry that was just created
        statusesEvt.fire(new Status(x.id, x.status, true, x.ref))
      }
    }

    val e = statuses.changed
    e.observe{x =>
      val listStatus: List[Status] = x.store.elements.toList
//      println(s"====== STATUSES CHANGED ======")
//      listStatus.foreach(x => println(x))

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
        peerPairsEvt.fire(new PeerPair(peerOne.id, peerTwo.id))

        // delete the elements in the list
        statusesEvt.fire(new Status(peerOne.id, true, true, peerOne.ref))
        statusesEvt.fire(new Status(peerTwo.id, true, true, peerTwo.ref))
      }
    }

    val contentPeers: Set[PeerPair] = peerPairs.now.store.elements
    contentPeers.foreach(x => println(x))

    val contentPeerStatus: Set[Status] = statuses.now.store.elements
    contentPeerStatus.foreach(x => println(x))

    document.body.appendChild(gridElem)
  }

  def drawNetwork(peerPairs: Signal[Dotted[AddWinsSet[PeerPair]]], canvasElem: Canvas, divCanvas: Div): Unit = {

    val pairs: Set[PeerPair] = peerPairs.now.store.elements
    var uniqueIds: Set[String] = Set()
    var peers: Set[Peer] = Set()

    pairs.foreach( pair => {
      uniqueIds = uniqueIds + (pair.left, pair.right)
    })

    val peersSize: Int = uniqueIds.size

    val ctx = canvasElem.getContext("2d").asInstanceOf[CanvasRenderingContext2D]

    // make the canvas not pixelated
    canvasElem.width = divCanvas.getBoundingClientRect().width.toInt
    //    canvasElem.height = divCanvas.getBoundingClientRect().height.toInt
    canvasElem.height = dom.window.innerHeight.toInt
    ctx.clearRect(0, 0, canvasElem.width, canvasElem.height)

    if(peersSize > 0){

      val imageSize = 100
      val offsetImage = imageSize/2
      val centerX = canvasElem.width/2 - offsetImage
      val centerY = canvasElem.height/2 - offsetImage
      val radius = Math.min(centerX, centerY)/2
      val distanceBetweenPeers: Int = 360 / peersSize
      var currentPosition: Double = 0

      // Create new Peer object from each unique IDs
      uniqueIds.toList.sorted.foreach(id => {
        val newPeer = new Peer(id, centerX + radius * sin(toRadians(currentPosition+distanceBetweenPeers)), centerY + radius*cos(toRadians(currentPosition+distanceBetweenPeers)))
        peers = peers + newPeer
        currentPosition = currentPosition + distanceBetweenPeers
      })

      ctx.lineWidth = 3
      ctx.strokeStyle = "green"

      // Make the connection lines
      ctx.beginPath()
      pairs.foreach(pair => {
        var peerLeft: Option[Peer] = None
        var peerRight: Option[Peer] = None
        peers.foreach(peer => {
          if (peer.id == pair.left || peer.id == pair.right) {
            if (peerLeft.isEmpty) peerLeft = Some(peer)
            else if (peerRight.isEmpty) peerRight = Some(peer)
          }
        })
        ctx.moveTo(peerLeft.get.x + imageSize / 2, peerLeft.get.y + imageSize / 2)
        ctx.lineTo(peerRight.get.x + imageSize / 2, peerRight.get.y + imageSize / 2)

      })
      ctx.stroke()

      // create the canvas
      val image = document.createElement("img").asInstanceOf[Image]
      image.src = "images/desktop.png"
      image.onload = (e: dom.Event) => {
        peers.foreach(peer => {
          ctx.drawImage(image, peer.x, peer.y, imageSize, imageSize)
          val peerText = if(peer.id == peerId) "You" else peer.id
          ctx.fillText(peerText, peer.x, peer.y, imageSize*5)
        })
//        ctx.drawImage(image, 0, 0, 100, 100)
//        ctx.drawImage(image, 600, 25, 100, 100)
//        ctx.drawImage(image, 300, 105, 100, 100)
      }



      //    ctx.beginPath()
      //    ctx.moveTo(100 / 2, 100 / 2)
      //    ctx.lineTo(600 + 100 / 2, 25 + 100 / 2)
      //
      //    ctx.moveTo(100 / 2, 100 / 2)
      //    ctx.lineTo(300 + 100 / 2, 105 + 100 / 2)

      //    ctx.moveTo(600 + 100 / 2, 25 + 100 / 2)
      //    ctx.lineTo(300 + 100 / 2, 105 + 100 / 2)
      //
      //    ctx.stroke()

    }
  }
}
