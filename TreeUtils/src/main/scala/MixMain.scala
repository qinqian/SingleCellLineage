package main.scala

import java.io.{File, PrintWriter}

import main.java.TreeParser
import main.scala.annotation.AnnotationsManager
import main.scala.mix._
import main.scala.node.{BestTree, RichNode}


/**
  * created by aaronmck on 2/13/14
  *
  * Copyright (c) 2014, aaronmck
  * All rights reserved.
  *
  * Redistribution and use in source and binary forms, with or without
  * modification, are permitted provided that the following conditions are met:
  *
  * 1. Redistributions of source code must retain the above copyright notice, this
  * list of conditions and the following disclaimer.
  * 2.  Redistributions in binary form must reproduce the above copyright notice,
  * this list of conditions and the following disclaimer in the documentation
  * and/or other materials provided with the distribution.
  *
  * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
  * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
  * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
  * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
  * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
  * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
  * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
  * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
  * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
  * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. 
  *
  */
case class MixConfig(allEventsFile: File = new File(MixMain.NOTAREALFILENAME),
                     outputTree: File = new File(MixMain.NOTAREALFILENAME),
                     mixRunLocation: File = new File(MixMain.NOTAREALFILENAME),
                     sample: String = "UNKNOWN",
                     firstX: Int = -1)

/**
  * process the full tree run from the original stats file to the final tree
  */
object MixMain extends App {
  val NOTAREALFILENAME = "/0192348102jr10234712930h8j19p0hjf129-348h512935"
  // please don't make a file with this name
  val NOTAREALFILE = new File(NOTAREALFILENAME)

  // parse the command line arguments
  val parser = new scopt.OptionParser[MixConfig]("MixMain") {
    head("MixToTree", "1.1")

    // *********************************** Inputs *******************************************************
    opt[File]("allEventsFile") required() valueName ("<file>") action { (x, c) => c.copy(allEventsFile = x) } text ("the input stats file")
    opt[File]("mixRunLocation") required() valueName ("<file>") action { (x, c) => c.copy(mixRunLocation = x) } text ("the tree to produce")
    opt[Int]("subsetFirstX") required() valueName ("<int>") action { (x, c) => c.copy(firstX = x) } text ("the tree to produce")
    opt[File]("outputTree") required() valueName ("<file>") action { (x, c) => c.copy(outputTree = x) } text ("the tree to produce")
    opt[String]("sample") required() valueName ("<file>") action { (x, c) => c.copy(sample = x) } text ("the tree to produce")

    // some general command-line setup stuff
    note("process a stats file into a JSON tree file\n")
    help("help") text ("prints the usage information you see here")
  }

  // *********************************** Run *******************************************************
  parser.parse(args, MixConfig()) map { config => {

    // parse the all events file into an object
    val readEventsObj = EventIO.readEventsObject(config.allEventsFile, config.sample)

    // load up any annotations we have
    val annotationMapping = new AnnotationsManager(readEventsObj)

    if (firstX > 0) {

    }

    val mixPackage: MixFilePackage = MixRunner.processIndividualMix(config, readEventsObj)


    // ------------------------------------------------------------
    // traverse the nodes and add names to any internal nodes without names
    // ------------------------------------------------------------


    // reassign the names
    val rootName = RichNode.recAssignNames(rootNode, mixParser)

    // now apply the parsimony results to the root of the tree (recursively walking down the nodes)
    RichNode.applyParsimonyGenotypes(rootNode, mixParser,readEventsObj.eventToCount.size)

    // check that the nodes we assigned are consistent
    RichNode.recCheckNodeConsistency(rootNode, mixParser)

    // count nodes before
    println("before collapsing nodes " + rootNode.countSubNodes())

    // collapse nodes from the root
    ParsimonyCollapser.collapseNodes(rootNode)

    // sort the nodes
    RichNode.reorderChildrenByAlleleString(rootNode)

    // add gray lines to branches where we're going to two identical alleles with different tissue sources
    RichNode.assignBranchColors(rootNode)

    // the updated numbers
    println("after collapsing nodes " + rootNode.countSubNodes())

    // assign the colors to the nodes
    RichNode.applyFunction(rootNode,annotationMapping.setNodeColor)

    // get an updated height to flip the tree around
    val maxHeight = RichNode.maxHeight(rootNode)

    // now output the adjusted tree
    val output = new PrintWriter(config.outputTree.getAbsolutePath)
    output.write("[{\n")
    output.write(RichNode.toJSONOutput(rootNode, None,1.0))
    output.write("}]\n")
    output.close()


  }} getOrElse {
    println("Unable to parse the command line arguments you passed in, please check that your parameters are correct")
  }


}