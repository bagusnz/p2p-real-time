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
      border := "1px solid black",
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

    val ctx = canvasElem.getContext("2d").asInstanceOf[CanvasRenderingContext2D]

    document.body.appendChild(gridElem)

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

    peerPairs.observe { peerPairs =>
//      println(s"====== PEERS CHANGED ======")
//      peerPairs.store.elements.toList.foreach(x => println(x))

      /**
       *  This block is to remove any peers that are in different clusters
       *  eg, this is p1, and peers are (p1,p2),(p4,p5),(p3,p2),(p3,p4)
       *  then p3 disconnects, the peers still saved (p4,p5) even though it is not connected to the same cluster with p1 anymore
       *  currently removal in the CRDT has not been realized due to the difference when peers are connecting with other peer
       *  the CRDT still have the peers that are connected but not in the same network anymore
       *
       *  Maybe use cleanup button?
       */
      var ids = scala.collection.mutable.Set[String]()
      findPeersInTheSameCluster(ids, peerId, peerPairs.store.elements.toList)
      val uniqueIds: Set[String] = peerPairs.store.elements.flatMap(pair => Set(pair.left, pair.right))
      val notInNetworkIds: List[String] = uniqueIds.toList.filter(id => !ids.contains(id))
      val peerPairsInCluster: Set[PeerPair] = peerPairs.store.elements.toList.filter(pp => !notInNetworkIds.contains(pp.left) && !notInNetworkIds.contains(pp.right)).toSet
      //      if(!isInConnectionProcess && notInNetworkIds.size > 0){
      //        notInNetworkIds.foreach( removeId => {
      //          println(s"removing $removeId")
      //          peerPairsEvt(new PeerPair(removeId, "disconnected"))
      //        })
      //      }

      DrawNetwork(peerPairsInCluster, canvasElem, divCanvas, ctx).draw();
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
  }

  /**
   * Recursive function to get the peerIds of those that are connected in the same cluster as this peer
   *
   * @param idSet: set that saves the peerIds
   * @param id: string of the peer id
   * @param peerPairs: the peer pairs from CRDT
   */
  def findPeersInTheSameCluster(idSet: scala.collection.mutable.Set[String], id: String, peerPairs: List[PeerPair]): Unit = {
    if (idSet.contains(id)){
      return
    }
    idSet += id
    val currentPeerPairs: List[PeerPair] = peerPairs.filter(pp => id == pp.left || id == pp.right)
    currentPeerPairs.foreach(pp => {
      val nextString: String = if (id == pp.left) pp.right else pp.left
      findPeersInTheSameCluster(idSet, nextString, peerPairs)
    })
  }

  

}
