package com.tessellation.demo.domain

import org.tessellation.kernel.Ω

case class DemoTransaction(txnid: String, resourceid: String, data1: Int) extends Ω
