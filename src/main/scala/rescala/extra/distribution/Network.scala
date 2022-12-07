package rescala.extra.distribution

import app.{PeerPair, Status}
import loci.registry.{Binding, Registry}
import loci.transmitter.RemoteRef
import kofre.base.Lattice
import rescala.default.*
import rescala.operator.Pulse
import app.WebApp.peerId

import scala.concurrent.Future

object Network {

  def replicate[A: Lattice](
                             signal: Signal[A],
                             deltaEvt: Option[Evt[Status]],
                             registry: Registry
  )(binding: Binding[A => Unit, A => Future[Unit]]) =
    distributePerRemote(_ => signal, deltaEvt, registry)(binding)

  def distributePerRemote[A: Lattice](
                                       signalFun: RemoteRef => Signal[A],
                                       deltaEvt: Option[Evt[Status]],
                                       registry: Registry
  )(binding: Binding[A => Unit, A => Future[Unit]]): Unit = {

    registry.bindSbj(binding) { (remoteRef: RemoteRef, newValue: A) =>
      val signal: Signal[A] = signalFun(remoteRef)
      val signalName        = signal.name.str
      // println(s"received value for $signalName: ${newValue.hashCode()}")
//      println(s"new value => $newValue")
      scheduler.forceNewTransaction(signal) { admissionTicket =>
        admissionTicket.recordChange(new InitialChange {
          override val source = signal
          override def writeValue(b: source.Value, v: source.Value => Unit): Boolean = {
            val merged = b.asInstanceOf[Pulse[A]].map(Lattice[A].merge(_, newValue)).asInstanceOf[source.Value]
            if (merged != b) {
              v(merged)
              true
            } else false
          }
        })
      }
    }

    var observers = Map[RemoteRef, Disconnectable]()

    def registerRemote(remoteRef: RemoteRef): Unit = {
      val signal: Signal[A] = signalFun(remoteRef)
      val signalName        = signal.name.str
      println(s"registering new remote $remoteRef (${remoteRef.toString}) for $signalName")

      deltaEvt match {
        case Some(evt) => evt.fire(new Status(peerId,true, false, remoteRef.toString))
        case None => println("deltaEvt not exists")
      }
      val remoteUpdate: A => Future[Unit] = {
        println(s"calling lookup on »${binding.name}«")
        registry.lookup(binding, remoteRef)
      }
      observers = observers + (remoteRef -> signal.observe { s =>
        // println(s"calling remote observer on $remoteRef for $signalName")
        if (remoteRef.connected) remoteUpdate(s)
        else observers(remoteRef).disconnect()
      })
    }

    registry.remotes.foreach(registerRemote)
    registry.remoteJoined.foreach(registerRemote)
    registry.remoteLeft.foreach { remoteRef =>
      println(s"removing remote $remoteRef")
      deltaEvt match {
        case Some(evt) => evt.fire(new Status(peerId, false, false, remoteRef.toString))
        case None => println("deltaEvt not exists")
      }
      observers(remoteRef).disconnect()
    }
  }

}
