package com.example.histour_androidaplication.models

import android.os.Parcel
import android.os.Parcelable

data class Utilizador(
    var id: String = "",
    var nome: String = "",
    var email: String = "",
    var tipo: String = "utilizador"
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(nome)
        parcel.writeString(email)
        parcel.writeString(tipo)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Utilizador> {
        override fun createFromParcel(parcel: Parcel): Utilizador = Utilizador(parcel)
        override fun newArray(size: Int): Array<Utilizador?> = arrayOfNulls(size)
    }
}
