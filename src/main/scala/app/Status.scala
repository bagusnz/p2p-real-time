package app

class Status(var id: String, var status: Boolean, var remove: Boolean, var ref: String) {

  def canEqual(a: Any) = a.isInstanceOf[Status]

  override def equals(that: Any): Boolean =
    that match {
      case that: Status => {
        that.canEqual(this) &&
          this.id == that.id && this.ref == that.ref
      }
      case _ => false
    }

  override def hashCode(): Int = {
    val prime = 31
    var result = 1
    result = prime * result + id.hashCode
    result = prime * result + ref.hashCode
    result
  }

  override def toString = s"StatusClass($id, $status, $remove, $ref)"
}
