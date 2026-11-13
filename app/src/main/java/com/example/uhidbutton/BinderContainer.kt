package com.example.uhidbutton

import android.os.IBinder
import android.os.Parcel
import android.os.Parcelable

class BinderContainer(val binder: IBinder) : Parcelable {
    protected constructor(p: Parcel) : this(p.readStrongBinder())
    override fun describeContents() = 0
    override fun writeToParcel(p: Parcel, flags: Int) = p.writeStrongBinder(binder)
    companion object CREATOR : Parcelable.Creator<BinderContainer> {
        override fun createFromParcel(p: Parcel) = BinderContainer(p)
        override fun newArray(size: Int) = arrayOfNulls<BinderContainer>(size)
    }
}
