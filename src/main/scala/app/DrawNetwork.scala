package app

import app.WebApp.peerId
import org.scalajs.dom
import org.scalajs.dom.{CanvasRenderingContext2D, document}
import org.scalajs.dom.html.{Canvas, Div, Image}

import scala.collection.mutable.ListBuffer
import scala.math.{cos, sin, toRadians}

class DrawNetwork(val pairs: Set[PeerPair], val canvasElem: Canvas, val divCanvas: Div, val ctx: CanvasRenderingContext2D) {

  private val uniqueIds: Set[String] = pairs.flatMap(pair => Set(pair.left, pair.right))
  private val indexToId: Map[Int, String] = uniqueIds.toList.sorted.zipWithIndex.map((id, index) => index -> id).toMap
  private val idToIndex: Map[String, Int] = uniqueIds.toList.sorted.zipWithIndex.map((id, index) => id -> index).toMap
  private val peersSize = uniqueIds.size
  private val connections: List[List[Int]] = pairs.map(pp => List(idToIndex.get(pp.left).get, idToIndex.get(pp.right).get)).toList

  // Variables for the function to find critical pairs
  private var disc: Array[Int] = new Array[Int](peersSize)
  private var low: Array[Int] = new Array[Int](peersSize)
  private var time: Int = 1
  private var criticalPairs: scala.collection.mutable.Set[PeerPair] = scala.collection.mutable.Set.empty
  private var pairMap: scala.collection.mutable.Map[Int, ListBuffer[Int]] = scala.collection.mutable.Map.empty

  /**
   * Function to find critical pairs.
   * If there are no critical pairs, meaning that every peer in the network is in a cycle
   * See Tarjan's Bridge-Finding Algorithm (TBFA)
   */
  private def criticalConnections(): Unit = {
    for(i <- 0 to peersSize-1){
      pairMap.put(i, ListBuffer[Int]())
    }
    connections.foreach(conn => {
      pairMap.get(conn(0)).get += conn(1)
      pairMap.get(conn(1)).get += conn(0)
    })
    dfs(0,-1)
  }

  /**
   * Recursive function to find the pairs that are critical
   *
   * @param curr
   * @param prev
   */
  private def dfs(curr: Int, prev: Int): Unit = {
    disc(curr) = time
    low(curr) = time
    time += 1

    pairMap.get(curr).get.foreach(next => {
      if(disc(next) == 0){
        dfs(next, curr)
        low(curr) = Math.min(low(curr), low(next))
      } else if (next != prev){
        low(curr) = Math.min(low(curr), disc(next))
      }
      if(low(next) > disc(curr)){
        criticalPairs += PeerPair(indexToId.get(curr).get, indexToId.get(next).get)
      }
    })
  }

  /**
   * Process and draw the topology of the p2p network
   */
  def draw(): Unit = {

    if(peersSize > 0){
      criticalConnections()
      println(s"pairMap=$pairMap")
      if(criticalPairs.size > 0){
        println(s"ans=$criticalPairs")
        criticalPairs.foreach(pair => println(s"critical edge = ${pair.left} -- ${pair.right}"))
      }
    }

    // The commented below is alternative to fixing the pixelated canvas, but currently not working when app is started in browsers in MacOS (firefox,chrome,etc)
    // Due to the NS_ERROR_FAILURE in javascript. Forum said cause changing canvas' width/height to a big value
    //    val dpr = dom.window.devicePixelRatio
    //    val canvasElemHeight = dom.window.getComputedStyle(canvasElem).getPropertyValue("height").dropRight(2)
    //    val canvasElemWidth = dom.window.getComputedStyle(canvasElem).getPropertyValue("width").dropRight(2)
    //    canvasElem.setAttribute("height", canvasElemHeight * dpr.toInt)
    //    canvasElem.setAttribute("width", canvasElemWidth * dpr.toInt)

    /* setting the canvas width and height based on the window (another alternative to make the canvas not pixelated */
    canvasElem.width = divCanvas.getBoundingClientRect().width.toInt
    canvasElem.height = dom.window.innerHeight.toInt
    //     canvasElem.height = divCanvas.getBoundingClientRect().height.toInt

    val imageSize = 100
    val offsetImage = imageSize / 2
    val centerX = canvasElem.width.toDouble / 2 - offsetImage
    val centerY = canvasElem.height.toDouble / 2 - offsetImage
    val radius = Math.min(centerX * 1.5, centerY * 1.5) / 2

    // return if the peer is not yet connected. Also show hint to connect
    if (peersSize == 0) {
      ctx.font = "20px sans-serif"
      ctx.clearRect(0, 0, canvasElem.width.toDouble, canvasElem.height.toDouble)
      ctx.fillText("Please connect to a peer", centerX - offsetImage, centerY + offsetImage, imageSize * 5)
      return
    }

    val distanceBetweenPeers: Int = 360 / peersSize

    // Create new Peer object from each unique IDs
    val peers: Map[String, Peer] = uniqueIds.toList.sorted.zipWithIndex.map((id, index) => {
      id -> Peer(id, centerX + radius * sin(toRadians(distanceBetweenPeers * index)), centerY + radius * cos(toRadians(distanceBetweenPeers * index)))
    }).toMap

    // do the drawing after image has been loaded
    val image = document.createElement("img").asInstanceOf[Image]
    image.src = "images/desktop.png"
    image.onload = (e: dom.Event) => {

      // reset the canvas
      ctx.clearRect(0, 0, canvasElem.width.toDouble, canvasElem.height.toDouble)

      // Make the connection lines
      ctx.lineWidth = 3
      ctx.strokeStyle = "green"
      ctx.font = "14px sans-serif"
      ctx.beginPath()
      pairs.foreach(pair => {
        val peerLeft: Peer = peers(pair.left)
        val peerRight: Peer = peers(pair.right)
        ctx.moveTo(peerLeft.x + imageSize / 2, peerLeft.y + imageSize / 2)
        ctx.lineTo(peerRight.x + imageSize / 2, peerRight.y + imageSize / 2)
      })
      ctx.stroke()

      // insert the peer image
      peers.foreach((id, peer) => {
        ctx.drawImage(image, peer.x, peer.y, imageSize, imageSize)
        val peerText = if (peer.id == peerId) "You" else peer.id
        ctx.fillText(peerText, peer.x, peer.y, imageSize * 5)
      })
    }
  }
}
