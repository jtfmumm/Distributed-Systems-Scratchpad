package dsscratch.algos.tarry

import dsscratch.components._
import dsscratch.clocks._
import randomific.Rand
import scala.collection.mutable.Queue
import scala.collection.mutable.ArrayBuffer
import dsscratch.util.Log

//Tarry's algorithm
//Kicked off by one initiator
//1) A process never forwards the token through the same channel twice
//2) A process only forwards the token to its parent when there is no other option


case class Token(id: Int)

case class ProcessToken(t: Token) extends Command


case class TNode(id: Int) extends Process {
  val clock = LogicalClock()
  val chs = ArrayBuffer[Channel]()
  val tokens = Queue[Token]()
  val finishedTokens = Queue[Token]()
  var log = Log()
  log.write(this + " log", clock.stamp())

  var parent: Process = EmptyProcess()
  var parentCh: Channel = Channel.empty
  var nonParentChsToSend = ArrayBuffer[Channel]()

  def recv(m: Message): Unit = {
    clock.compareAndUpdate(m.ts)
    log.write("Receiving " + m, clock.stamp())
    m.cmd match {
      case ProcessToken(t) => processToken(t, m.sender)
      case _ =>
    }
  }

  def step(): Unit = {
    if (tokens.isEmpty && finishedTokens.isEmpty) return
    nonParentChsToSend.size match {
      case 0 if hasNoParent && tokens.nonEmpty => {
        val t = tokens.dequeue()
        finishedTokens.enqueue(t)
      }
      case 0 if hasNoParent =>
      case 0 => {
        sendToken(parentCh)
        val t = tokens.dequeue() //Parent is the last destination for token
        finishedTokens.enqueue(t)
        emptyParent()
      }
      case _ => {
        val randChIndex = Rand.rollFromZero(nonParentChsToSend.size)
        val ch = nonParentChsToSend.remove(randChIndex)
        sendToken(ch)
      }
    }
  }

  def addChannel(ch: Channel): Unit = {
    if (!chs.contains(ch)) chs.append(ch)
  }

  def removeChannel(ch: Channel): Unit = {
    if (!chs.contains(ch)) return
    val i = chs.indexOf(ch)
    chs.remove(i)
  }

  def initiate(t: Token): Unit = {
    tokens.enqueue(t)
    log.write("Initiator: No Parent", clock.stamp())
    nonParentChsToSend = ArrayBuffer(chs.filter(_ != parentCh): _*)
    val firstCh: Channel = Rand.pickItem(chs)
    val cmd = ProcessToken(t)
    val msg = Message(cmd, this, clock.stamp())
    log.write("Sending on " + firstCh, msg.ts)
    firstCh.recv(msg)
    val firstChIndex = nonParentChsToSend.indexOf(firstCh)
    nonParentChsToSend.remove(firstChIndex)
  }

  private def hasNoParent: Boolean = parentCh == Channel.empty

  private def sendToken(ch: Channel) = {
    val pt = ProcessToken(tokens.last)
    val msg = Message(pt, this, clock.stamp())
    log.write("Sending on " + ch, msg.ts)
    ch.recv(msg)
  }

  private def processToken(t: Token, sender: Process): Unit = {
    if (tokens.isEmpty && parentCh == Channel.empty && finishedTokens.isEmpty) {
      parent = sender
      log.write("Parent: " + parent, clock.stamp())
      parentCh = chs.filter(_.hasTarget(sender))(0)
      nonParentChsToSend = ArrayBuffer(chs.filter(_ != parentCh): _*)
      tokens.enqueue(t)
    }
  }

  private def emptyParent(): Unit = {
    parentCh = Channel.empty
  }

  override def toString: String = "TNode " + id
}

object Tarry {
  def runFor(nodeCount: Int, density: Double) = {
    assert(density >= 0 && density <= 1)
    val nodes = (1 to nodeCount).map(x => TNode(x))

    val maxEdges = (nodeCount * (nodeCount - 1)) - nodeCount //Rule out self connections
    val possibleExtras = maxEdges - (nodeCount - 1) //Topology must be connected, so we need at least one path of n - 1 edges

    val extras = (possibleExtras * density).floor.toInt

    val topology: Topology[TNode] = Topology.connectedWithKMoreEdges(extras, nodes)

    topology.nodes(0).initiate(Token(1))
    println("*******TOPOLOGY*********")
    println(topology.nodes)
    for (ch <- topology.chs) println(ch)
    println("************************")
    while (topology.nodes.exists(nd => nd.finishedTokens.isEmpty)) {
      for (nd <- topology.nodes) nd.step()
      for (ch <- topology.chs) ch.step()
    }
    for (nd <- topology.nodes) {
      println("Next")
      println(nd.log)
    }
  }
}