package app

class Peer(var left: String, var right: String) {

  def consistsString(s: String): Boolean = {
    this.left == s || this.right == s
  }

  def canEqual(a: Any) = a.isInstanceOf[Peer]

  override def equals(that: Any): Boolean =
    that match {
      case that: Peer => {
        that.canEqual(this) &&
          (
            (this.left == that.left &&
              this.right == that.right)
              ||
            (this.left == that.right &&
              this.right == that.left)
          )
      }
      case _ => false
    }

  override def hashCode(): Int = {
    val prime = 31
    var result = 1
    result = prime * result + left.hashCode;
    result = prime * result + right.hashCode
    result
  }

  override def toString = s"PeerClass($left, $right)"
}
