package com.overtime.camera.itemsame

interface ItemSame<T> {

    fun sameAs(same: T): Boolean
    fun contentsSameAs(same: T): Boolean
}