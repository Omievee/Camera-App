package com.itsovertime.overtimecamera.play.itemsame

interface ItemSame<T> {

    fun sameAs(same: T): Boolean
    fun contentsSameAs(same: T): Boolean
}