package app

import com.github.plokhotnyuk.jsoniter_scala.core.{JsonValueCodec, readFromString, writeToString}
import org.scalajs.dom
import rescala.default._

object Storing {
  def storedAs[A: JsonValueCodec](key: String, default: => A)(create: A => Signal[A]): Signal[A] = {
    val init: A = {
      val item = dom.window.localStorage.getItem(key)
      try { readFromString[A](item)}
      catch {
        case cause: Throwable =>
          println(s"could not restore $key: $cause")
          dom.window.localStorage.removeItem(key)
          default
      }
    }
    val sig = create(init)
    sig.observe(
      {(ft: A) =>
        println(s"storing $key")
        dom.window.localStorage.setItem(key, writeToString(ft))
      },
      fireImmediately = false
    )
    sig
  }
}
