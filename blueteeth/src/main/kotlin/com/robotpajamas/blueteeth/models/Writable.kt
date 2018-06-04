package com.robotpajamas.blueteeth.models

import java.util.*

typealias WriteHandler = ((Result<Boolean>) -> Unit)

interface Writable : Connectable {
    enum class Type { WITH_RESPONSE, WITHOUT_RESPONSE }

    fun write(data: ByteArray,
              characteristic: UUID,
              service: UUID,
              type: Type = Type.WITHOUT_RESPONSE,
              block: WriteHandler? = null)
}